package io.stablepay.api.infrastructure.trino;

import lombok.Builder;

@Builder(toBuilder = true)
public record TrinoStuckCursor(long stuckMillis, String transactionId) {}
