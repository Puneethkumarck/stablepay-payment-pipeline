package io.stablepay.flink.agg;

import lombok.Builder;

@Builder(toBuilder = true)
record VolumeAccumulator(
        long totalAmountMicros,
        long transactionCount,
        String flowType,
        String direction,
        String currencyCode) {}
