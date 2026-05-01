package io.stablepay.flink.transition;

import io.stablepay.flink.model.ValidatedEvent;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;

public class TransitionValidator {

  private static final String ENV_STRICT_TRANSITIONS = "STBLPAY_FLINK_STRICT_TRANSITIONS";

  public static boolean isStrictMode() {
    return "true".equalsIgnoreCase(System.getenv().getOrDefault(ENV_STRICT_TRANSITIONS, "false"));
  }

  public static ValueStateDescriptor<String> lastStatusDescriptor() {
    return new ValueStateDescriptor<>("last-status", Types.STRING);
  }

  public static ValidationOutcome validate(ValueState<String> lastStatusState, ValidatedEvent event)
      throws Exception {
    var currentStatus = extractStatus(event);
    if (currentStatus == null) {
      return new ValidationOutcome.Valid(null, null);
    }

    var lastStatus = lastStatusState.value();

    if (lastStatus == null) {
      lastStatusState.update(currentStatus);
      return new ValidationOutcome.FirstEvent(currentStatus);
    }

    if (lastStatus.equals(currentStatus)) {
      return new ValidationOutcome.Valid(lastStatus, currentStatus);
    }

    var valid = TransitionGraph.isValidTransition(event.topic(), lastStatus, currentStatus);
    if (valid) {
      lastStatusState.update(currentStatus);
      return new ValidationOutcome.Valid(lastStatus, currentStatus);
    }
    return new ValidationOutcome.Invalid(lastStatus, currentStatus);
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
