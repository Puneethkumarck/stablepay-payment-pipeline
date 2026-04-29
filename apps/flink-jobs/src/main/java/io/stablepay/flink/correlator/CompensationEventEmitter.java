package io.stablepay.flink.correlator;

import org.apache.avro.generic.GenericRecord;

public final class CompensationEventEmitter {

    private CompensationEventEmitter() {}

    public static boolean shouldTrigger(FlowState state) {
        return state.anyLegCompleted() && state.anyLegFailed()
                && !"COMPENSATION_INITIATED".equals(state.currentFlowStatus())
                && !"COMPENSATION_COMPLETED".equals(state.currentFlowStatus());
    }

    public static GenericRecord emitCompensationInitiated(FlowState state) {
        return FlowLifecycleEmitter.emit(state, "COMPENSATION_INITIATED");
    }

    public static GenericRecord emitCompensationCompleted(FlowState state) {
        return FlowLifecycleEmitter.emit(state, "COMPENSATION_COMPLETED");
    }
}
