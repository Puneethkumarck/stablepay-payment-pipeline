package io.stablepay.flink.agg;

import lombok.Builder;

@Builder(toBuilder = true)
record DlqAccumulator(
        long eventCount,
        int maxRetryCount,
        String errorClass,
        String sourceTopic) {}
