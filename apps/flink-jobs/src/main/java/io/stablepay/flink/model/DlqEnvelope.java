package io.stablepay.flink.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record DlqEnvelope(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String errorClass,
    String errorMessage,
    byte[] originalPayloadBytes,
    long failedAt,
    int retryCount
) {
    public DlqEnvelope {
        originalPayloadBytes = originalPayloadBytes != null ? originalPayloadBytes.clone() : null;
    }
}
