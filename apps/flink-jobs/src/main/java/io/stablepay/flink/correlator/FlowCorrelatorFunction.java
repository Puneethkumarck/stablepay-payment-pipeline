package io.stablepay.flink.correlator;

import java.time.Duration;
import java.util.Objects;

import org.apache.avro.generic.GenericRecord;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import io.stablepay.flink.model.ValidatedEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlowCorrelatorFunction extends KeyedProcessFunction<String, ValidatedEvent, GenericRecord> {

    private transient ValueState<FlowState> flowState;

    @Override
    public void open(OpenContext openContext) throws Exception {
        ValueStateDescriptor<FlowState> descriptor =
                new ValueStateDescriptor<>("flow-state", FlowState.class);

        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Duration.ofHours(24))
                .setUpdateType(StateTtlConfig.UpdateType.OnReadAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        descriptor.enableTimeToLive(ttlConfig);

        flowState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(ValidatedEvent event, Context ctx, Collector<GenericRecord> out)
            throws Exception {
        if (event.flowId() == null || event.flowId().isEmpty()) {
            return;
        }

        FlowState state = flowState.value();
        if (state == null) {
            state = new FlowState();
        }

        String topic = event.topic();
        String previousStatus = state.currentFlowStatus();

        if ("payment.flow.v1".equals(topic)) {
            handleFlowEvent(state, event);
        } else {
            handleChildEvent(state, event, topic);
        }

        String newStatus = state.deriveFlowStatus();
        state.setCurrentFlowStatus(newStatus);
        flowState.update(state);

        if (!Objects.equals(newStatus, previousStatus)) {
            out.collect(FlowLifecycleEmitter.emit(state, newStatus));
            log.info("flow_lifecycle: flow_id={} status={}->{}", event.flowId(), previousStatus, newStatus);

            if ("PARTIALLY_COMPLETED".equals(newStatus)) {
                state.setCurrentFlowStatus("COMPENSATION_INITIATED");
                flowState.update(state);
                out.collect(FlowLifecycleEmitter.emit(state, "COMPENSATION_INITIATED"));
                log.info("compensation_initiated: flow_id={}", event.flowId());
            }
        }
    }

    private void handleFlowEvent(FlowState state, ValidatedEvent event) {
        var record = event.record();
        var flowStatus = record.get("flow_status");
        if (flowStatus != null && "INITIATED".equals(flowStatus.toString())) {
            var customerId = record.get("customer_id");
            var flowType = record.get("flow_type");
            state.initialize(
                    event.flowId(),
                    customerId != null ? customerId.toString() : "",
                    flowType != null ? flowType.toString() : "ONRAMP",
                    event.eventTimeMillis());
        }
    }

    private void handleChildEvent(FlowState state, ValidatedEvent event, String topic) {
        if (state.flowId() == null) {
            state.initialize(event.flowId(), "", "ONRAMP", event.eventTimeMillis());
        }

        String legType = classifyLegType(topic);
        String legId = legType + "-" + event.flowId();
        String status = extractChildStatus(event);
        String reference = extractReference(event);

        state.updateLeg(legId, legType, status, reference);
    }

    private static String classifyLegType(String topic) {
        if (topic.contains("payin")) return "PAYIN";
        if (topic.contains("payout")) return "PAYOUT";
        if (topic.contains("chain.transaction")) return "TRADE";
        return "TRADE";
    }

    private static String extractChildStatus(ValidatedEvent event) {
        var record = event.record();
        for (String field : new String[]{"internal_status", "status", "flow_status"}) {
            var val = record.get(field);
            if (val != null) return val.toString();
        }
        return "UNKNOWN";
    }

    private static String extractReference(ValidatedEvent event) {
        var record = event.record();
        for (String field : new String[]{"payout_reference", "payin_reference", "tx_hash"}) {
            var val = record.get(field);
            if (val != null) return val.toString();
        }
        return event.eventId();
    }
}
