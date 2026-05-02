package io.stablepay.api.infrastructure.idempotency.fixtures;

import io.stablepay.api.domain.model.CachedResponse;
import java.time.Instant;

public final class IdempotencyFixtures {

  public static final String SOME_IDEMPOTENCY_KEY = "idem-key-001";
  public static final Instant SOME_NOW = Instant.parse("2026-05-01T10:00:00Z");
  public static final Instant SOME_EXPIRES_AT = Instant.parse("2026-05-02T10:00:00Z");
  public static final byte[] SOME_RESPONSE_BODY = "{\"status\":\"ACCEPTED\"}".getBytes();

  public static final CachedResponse SOME_CACHED_RESPONSE =
      CachedResponse.builder()
          .status(200)
          .body(SOME_RESPONSE_BODY)
          .expiresAt(SOME_EXPIRES_AT)
          .build();

  private IdempotencyFixtures() {}
}
