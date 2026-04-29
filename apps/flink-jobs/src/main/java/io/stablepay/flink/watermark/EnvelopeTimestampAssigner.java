package io.stablepay.flink.watermark;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;

import io.stablepay.flink.model.ValidatedEvent;

public class EnvelopeTimestampAssigner implements SerializableTimestampAssigner<ValidatedEvent> {

    @Override
    public long extractTimestamp(ValidatedEvent event, long recordTimestamp) {
        return event.eventTimeMillis();
    }
}
