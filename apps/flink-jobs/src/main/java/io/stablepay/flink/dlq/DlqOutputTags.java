package io.stablepay.flink.dlq;

import io.stablepay.flink.model.DlqEnvelope;
import org.apache.flink.util.OutputTag;

public final class DlqOutputTags {

  public static final OutputTag<DlqEnvelope> SCHEMA_INVALID =
      new OutputTag<>("dlq-schema-invalid") {};

  public static final OutputTag<DlqEnvelope> LATE_EVENT = new OutputTag<>("dlq-late-event") {};

  public static final OutputTag<DlqEnvelope> ILLEGAL_TRANSITION =
      new OutputTag<>("dlq-illegal-transition") {};

  public static final OutputTag<DlqEnvelope> SINK_FAILURE = new OutputTag<>("dlq-sink-failure") {};

  private DlqOutputTags() {}
}
