package io.stablepay.flink.model;

public record DlqEnvelope(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String errorClass,
    String errorMessage,
    byte[] originalPayloadBytes,
    long failedAt,
    int retryCount
) {}
