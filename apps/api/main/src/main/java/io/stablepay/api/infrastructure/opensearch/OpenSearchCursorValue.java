package io.stablepay.api.infrastructure.opensearch;

import lombok.Builder;

@Builder(toBuilder = true)
public record OpenSearchCursorValue(long eventTimeMillis, String eventId) {}
