package io.stablepay.flink.transition;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;

import io.stablepay.flink.model.ValidatedEvent;

public class TransitionValidator {

    private static final String ENV_STRICT_TRANSITIONS = "STBLPAY_FLINK_STRICT_TRANSITIONS";

    public enum TransitionResult {
        VALID,
        INVALID,
        FIRST_EVENT
    }

    public record ValidationOutcome(TransitionResult result, String fromStatus, String toStatus) {}

    public static boolean isStrictMode() {
        return "true".equalsIgnoreCase(System.getenv().getOrDefault(ENV_STRICT_TRANSITIONS, "false"));
    }

    public static ValueStateDescriptor<String> lastStatusDescriptor() {
        return new ValueStateDescriptor<>("last-status", Types.STRING);
    }

    public static ValidationOutcome validate(ValueState<String> lastStatusState, ValidatedEvent event)
            throws Exception {
        String currentStatus = extractStatus(event);
        if (currentStatus == null) {
            return new ValidationOutcome(TransitionResult.VALID, null, null);
        }

        String lastStatus = lastStatusState.value();

        if (lastStatus == null) {
            lastStatusState.update(currentStatus);
            return new ValidationOutcome(TransitionResult.FIRST_EVENT, null, currentStatus);
        }

        if (lastStatus.equals(currentStatus)) {
            return new ValidationOutcome(TransitionResult.VALID, lastStatus, currentStatus);
        }

        boolean valid = TransitionGraph.isValidTransition(event.topic(), lastStatus, currentStatus);
        if (valid) {
            lastStatusState.update(currentStatus);
        }
        return new ValidationOutcome(
                valid ? TransitionResult.VALID : TransitionResult.INVALID,
                lastStatus,
                currentStatus);
    }

    private static String extractStatus(ValidatedEvent event) {
        var record = event.toRecord();
        var status = record.get("internal_status");
        if (status != null) return status.toString();
        status = record.get("status");
        if (status != null) return status.toString();
        status = record.get("flow_status");
        if (status != null) return status.toString();
        return null;
    }
}
