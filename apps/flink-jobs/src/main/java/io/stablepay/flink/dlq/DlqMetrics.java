package io.stablepay.flink.dlq;

import java.io.Serializable;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.Counter;

public class DlqMetrics implements Serializable {

    private final Counter schemaInvalidCounter;
    private final Counter lateEventCounter;
    private final Counter illegalTransitionCounter;
    private final Counter sinkFailureCounter;

    public DlqMetrics(RuntimeContext runtimeContext) {
        var metricGroup = runtimeContext.getMetricGroup();
        this.schemaInvalidCounter = metricGroup.counter("dlq_schema_invalid_total");
        this.lateEventCounter = metricGroup.counter("dlq_late_event_total");
        this.illegalTransitionCounter = metricGroup.counter("dlq_illegal_transition_total");
        this.sinkFailureCounter = metricGroup.counter("dlq_sink_failure_total");
    }

    public void incrementSchemaInvalid() {
        schemaInvalidCounter.inc();
    }

    public void incrementLateEvent() {
        lateEventCounter.inc();
    }

    public void incrementIllegalTransition() {
        illegalTransitionCounter.inc();
    }

    public void incrementSinkFailure() {
        sinkFailureCounter.inc();
    }
}
