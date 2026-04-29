package io.stablepay.flink.dlq;

import java.time.Instant;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;

public final class DlqRouter {

    private DlqRouter() {}

    public static DlqEnvelope schemaInvalid(
            String topic, int partition, long offset, byte[] rawBytes, String errorMsg) {
        return new DlqEnvelope(
                topic, partition, offset, "SCHEMA_INVALID", errorMsg,
                rawBytes, Instant.now().toEpochMilli(), 0);
    }

    public static DlqEnvelope lateEvent(ValidatedEvent event, long watermark, String errorMsg) {
        return new DlqEnvelope(
                event.topic(), 0, 0, "LATE_EVENT",
                errorMsg + " (event_time=" + event.eventTimeMillis() + ", watermark=" + watermark + ")",
                null, Instant.now().toEpochMilli(), 0);
    }

    public static DlqEnvelope illegalTransition(
            ValidatedEvent event, String fromStatus, String toStatus) {
        return new DlqEnvelope(
                event.topic(), 0, 0, "ILLEGAL_TRANSITION",
                "Invalid transition: " + fromStatus + " -> " + toStatus,
                null, Instant.now().toEpochMilli(), 0);
    }

    public static DlqEnvelope sinkFailure(
            ValidatedEvent event, String sinkType, String errorMsg, int retryCount) {
        return new DlqEnvelope(
                event.topic(), 0, 0, "SINK_FAILURE",
                sinkType + ": " + errorMsg,
                null, Instant.now().toEpochMilli(), retryCount);
    }
}
