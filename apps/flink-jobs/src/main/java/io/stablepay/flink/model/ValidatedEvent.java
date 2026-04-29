package io.stablepay.flink.model;

import org.apache.avro.generic.GenericRecord;

public record ValidatedEvent(
    String topic,
    String key,
    GenericRecord record,
    String eventId,
    long eventTimeMillis,
    String flowId,
    String schemaVersion
) {}
