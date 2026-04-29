package io.stablepay.flink.process;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.stablepay.flink.dlq.DlqMetrics;
import io.stablepay.flink.dlq.DlqOutputTags;
import io.stablepay.flink.dlq.DlqRouter;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.transition.TransitionValidator;

public class ValidateAndRouteFunction extends KeyedProcessFunction<String, ValidatedEvent, ValidatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ValidateAndRouteFunction.class);
    private static final long LATE_EVENT_BUFFER_MS = 60_000;

    private transient ValueState<String> lastStatusState;
    private transient DlqMetrics dlqMetrics;
    private transient boolean strictMode;

    @Override
    public void open(OpenContext openContext) throws Exception {
        lastStatusState = getRuntimeContext().getState(TransitionValidator.lastStatusDescriptor());
        dlqMetrics = new DlqMetrics(getRuntimeContext());
        strictMode = TransitionValidator.isStrictMode();
    }

    @Override
    public void processElement(ValidatedEvent event, Context ctx, Collector<ValidatedEvent> out) throws Exception {
        long watermark = ctx.timerService().currentWatermark();
        if (watermark > Long.MIN_VALUE && event.eventTimeMillis() < watermark - LATE_EVENT_BUFFER_MS) {
            var dlq = DlqRouter.lateEvent(event, watermark, "Event arrived after watermark buffer");
            ctx.output(DlqOutputTags.LATE_EVENT, dlq);
            dlqMetrics.incrementLateEvent();
            if (strictMode) return;
        }

        var outcome = TransitionValidator.validate(lastStatusState, event);
        if (outcome.result() == TransitionValidator.TransitionResult.INVALID) {
            var dlq = DlqRouter.illegalTransition(event, outcome.fromStatus(), outcome.toStatus());
            ctx.output(DlqOutputTags.ILLEGAL_TRANSITION, dlq);
            dlqMetrics.incrementIllegalTransition();
            log.warn("illegal_transition: topic={} from={} to={}", event.topic(), outcome.fromStatus(), outcome.toStatus());
            if (strictMode) return;
        }

        out.collect(event);
    }
}
