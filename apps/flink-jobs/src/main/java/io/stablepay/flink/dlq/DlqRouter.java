package io.stablepay.flink.dlq;

import java.time.Instant;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;

public final class DlqRouter {

    private DlqRouter() {}

    public static DlqEnvelope schemaInvalid(
            String topic, int partition, long offset, byte[] rawBytes, String errorMsg) {
        return DlqEnvelope.builder()
                .sourceTopic(topic)
                .sourcePartition(partition)
                .sourceOffset(offset)
                .errorClass("SCHEMA_INVALID")
                .errorMessage(errorMsg)
                .originalPayloadBytes(rawBytes)
                .failedAt(Instant.now().toEpochMilli())
                .retryCount(0)
                .build();
    }

    public static DlqEnvelope lateEvent(ValidatedEvent event, long watermark, String errorMsg) {
        return DlqEnvelope.builder()
                .sourceTopic(event.topic())
                .sourcePartition(event.sourcePartition())
                .sourceOffset(event.sourceOffset())
                .errorClass("LATE_EVENT")
                .errorMessage(errorMsg + " (event_time=" + event.eventTimeMillis() + ", watermark=" + watermark + ")")
                .originalPayloadBytes(event.recordBytes())
                .failedAt(Instant.now().toEpochMilli())
                .retryCount(0)
                .build();
    }

    public static DlqEnvelope illegalTransition(
            ValidatedEvent event, String fromStatus, String toStatus) {
        return DlqEnvelope.builder()
                .sourceTopic(event.topic())
                .sourcePartition(event.sourcePartition())
                .sourceOffset(event.sourceOffset())
                .errorClass("ILLEGAL_TRANSITION")
                .errorMessage("Invalid transition: " + fromStatus + " -> " + toStatus)
                .originalPayloadBytes(event.recordBytes())
                .failedAt(Instant.now().toEpochMilli())
                .retryCount(0)
                .build();
    }

    public static DlqEnvelope sinkFailure(
            ValidatedEvent event, String sinkType, String errorMsg, int retryCount) {
        return DlqEnvelope.builder()
                .sourceTopic(event.topic())
                .sourcePartition(event.sourcePartition())
                .sourceOffset(event.sourceOffset())
                .errorClass("SINK_FAILURE")
                .errorMessage(sinkType + ": " + errorMsg)
                .originalPayloadBytes(event.recordBytes())
                .failedAt(Instant.now().toEpochMilli())
                .retryCount(retryCount)
                .build();
    }
}
