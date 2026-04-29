package io.stablepay.flink.correlator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;

import io.stablepay.events.flow.PaymentFlowV1;

public final class FlowLifecycleEmitter {

    private static final Schema SCHEMA = PaymentFlowV1.getClassSchema();
    private static final Schema ENVELOPE_SCHEMA = SCHEMA.getField("envelope").schema();
    private static final Schema LEG_SCHEMA = SCHEMA.getField("legs").schema().getElementType();

    private FlowLifecycleEmitter() {}

    public static byte[] serializeToBytes(FlowState state, String newStatus) {
        var record = emit(state, newStatus);
        try {
            var out = new ByteArrayOutputStream();
            var writer = new GenericDatumWriter<GenericRecord>(SCHEMA);
            var encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize flow lifecycle event", e);
        }
    }

    static GenericRecord emit(FlowState state, String newStatus) {
        var now = System.currentTimeMillis();

        var envelope = new GenericData.Record(ENVELOPE_SCHEMA);
        envelope.put("event_id", UUID.randomUUID().toString());
        envelope.put("event_time", now);
        envelope.put("ingest_time", now);
        envelope.put("schema_version", "1.0.0");
        envelope.put("flow_id", state.flowId());
        envelope.put("correlation_id", null);
        envelope.put("trace_id", null);

        var legs = new ArrayList<GenericRecord>();
        for (var leg : state.toLegList()) {
            var legRecord = new GenericData.Record(LEG_SCHEMA);
            legRecord.put("leg_id", leg.legId());
            legRecord.put("leg_type", leg.legType());
            legRecord.put("status", leg.status());
            legRecord.put("reference", leg.reference());
            legs.add(legRecord);
        }

        var flow = new GenericData.Record(SCHEMA);
        flow.put("envelope", envelope);
        flow.put("flow_id", state.flowId());
        flow.put("customer_id", state.customerId());
        flow.put("flow_type", new GenericData.EnumSymbol(
                SCHEMA.getField("flow_type").schema(), state.flowType()));
        flow.put("flow_status", new GenericData.EnumSymbol(
                SCHEMA.getField("flow_status").schema(), newStatus));
        flow.put("source_amount", null);
        flow.put("target_amount", null);
        flow.put("fx_rate", null);
        flow.put("legs", legs);
        flow.put("description", null);

        return flow;
    }
}
