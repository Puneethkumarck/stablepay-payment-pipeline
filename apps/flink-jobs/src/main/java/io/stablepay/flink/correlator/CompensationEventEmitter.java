package io.stablepay.flink.correlator;

public final class CompensationEventEmitter {

    private CompensationEventEmitter() {}

    public static boolean shouldTrigger(FlowState state) {
        return state.anyLegCompleted() && state.anyLegFailed()
                && !"COMPENSATION_INITIATED".equals(state.currentFlowStatus())
                && !"COMPENSATION_COMPLETED".equals(state.currentFlowStatus());
    }

    public static byte[] emitCompensationInitiated(FlowState state) {
        return FlowLifecycleEmitter.serializeToBytes(state, "COMPENSATION_INITIATED");
    }

    public static byte[] emitCompensationCompleted(FlowState state) {
        return FlowLifecycleEmitter.serializeToBytes(state, "COMPENSATION_COMPLETED");
    }
}
