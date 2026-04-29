package io.stablepay.flink.model;

public sealed interface ValidationResult {

    record Valid(ValidatedEvent event) implements ValidationResult {}

    record Invalid(DlqEnvelope dlqEnvelope) implements ValidationResult {}
}
