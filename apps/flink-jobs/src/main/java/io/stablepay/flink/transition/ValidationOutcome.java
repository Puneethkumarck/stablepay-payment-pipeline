package io.stablepay.flink.transition;

public sealed interface ValidationOutcome {

  record Valid(String fromStatus, String toStatus) implements ValidationOutcome {}

  record Invalid(String fromStatus, String toStatus) implements ValidationOutcome {}

  record FirstEvent(String toStatus) implements ValidationOutcome {}
}
