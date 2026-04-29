package io.stablepay.flink.deser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.avro.generic.GenericRecord;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;

public final class EnvelopeValidator {

    private static final long MAX_FUTURE_SKEW_MILLIS = ChronoUnit.HOURS.getDuration().toMillis();

    private EnvelopeValidator() {}

    public static ValidationResult validate(
            String topic, int partition, long offset, String key, GenericRecord record, byte[] rawBytes) {
        var envelope = record.get("envelope");
        if (envelope == null) {
            return toDlq(topic, partition, offset, "MISSING_ENVELOPE", "Record has no envelope field", rawBytes);
        }

        if (!(envelope instanceof GenericRecord envelopeRecord)) {
            return toDlq(topic, partition, offset, "INVALID_ENVELOPE", "Envelope is not a record", rawBytes);
        }

        var eventId = envelopeRecord.get("event_id");
        if (eventId == null || eventId.toString().isBlank()) {
            return toDlq(topic, partition, offset, "MISSING_EVENT_ID", "Envelope event_id is null or blank", rawBytes);
        }

        var eventTime = envelopeRecord.get("event_time");
        if (eventTime == null || !(eventTime instanceof Long eventTimeMillis)) {
            return toDlq(
                    topic, partition, offset, "MISSING_EVENT_TIME", "Envelope event_time is null or not a long", rawBytes);
        }

        if (eventTimeMillis <= 0) {
            return toDlq(
                    topic, partition, offset, "INVALID_EVENT_TIME", "Envelope event_time is not positive", rawBytes);
        }

        long now = Instant.now().toEpochMilli();
        if (eventTimeMillis > now + MAX_FUTURE_SKEW_MILLIS) {
            return toDlq(
                    topic,
                    partition,
                    offset,
                    "FUTURE_EVENT_TIME",
                    "Envelope event_time is more than 1 hour in the future",
                    rawBytes);
        }

        var schemaVersion = envelopeRecord.get("schema_version");
        var flowId = envelopeRecord.get("flow_id");

        return new ValidationResult.Valid(new ValidatedEvent(
                topic,
                key,
                record,
                eventId.toString(),
                eventTimeMillis,
                flowId != null ? flowId.toString() : null,
                schemaVersion != null ? schemaVersion.toString() : "unknown"));
    }

    private static ValidationResult.Invalid toDlq(
            String topic, int partition, long offset, String errorClass, String errorMessage, byte[] rawBytes) {
        return new ValidationResult.Invalid(
                new DlqEnvelope(topic, partition, offset, errorClass, errorMessage, rawBytes, Instant.now().toEpochMilli(), 0));
    }
}
