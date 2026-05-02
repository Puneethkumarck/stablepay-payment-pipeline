package io.stablepay.api.infrastructure.cursor;

import lombok.Builder;

@Builder(toBuilder = true)
public record Base64PipeCursorPart(long longPart, String stringPart) {}
