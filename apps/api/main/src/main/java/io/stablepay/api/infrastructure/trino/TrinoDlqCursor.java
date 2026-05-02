package io.stablepay.api.infrastructure.trino;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record TrinoDlqCursor(Instant failedAt, String dlqId) {}
