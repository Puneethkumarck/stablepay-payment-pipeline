package io.stablepay.flink.watermark;

import io.stablepay.flink.model.ValidatedEvent;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;

public class EnvelopeTimestampAssigner implements SerializableTimestampAssigner<ValidatedEvent> {

  @Override
  public long extractTimestamp(ValidatedEvent event, long recordTimestamp) {
    return event.eventTimeMillis();
  }
}
