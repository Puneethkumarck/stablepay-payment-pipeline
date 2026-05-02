package io.stablepay.api.infrastructure.trino;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record TrinoFlowCursor(Instant createdAt, String id) {}
