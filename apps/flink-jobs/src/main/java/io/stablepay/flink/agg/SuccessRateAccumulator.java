package io.stablepay.flink.agg;

import lombok.Builder;

@Builder(toBuilder = true)
record SuccessRateAccumulator(
        long totalCount,
        long completedCount,
        long failedCount,
        String flowType) {}
