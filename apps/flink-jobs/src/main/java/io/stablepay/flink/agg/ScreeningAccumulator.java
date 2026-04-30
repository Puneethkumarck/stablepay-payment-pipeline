package io.stablepay.flink.agg;

import lombok.Builder;

@Builder(toBuilder = true)
record ScreeningAccumulator(
        long totalCount,
        long totalDurationMs,
        double totalScore,
        long scoreCount,
        String outcome,
        String provider) {}
