package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record CachedResponse(int status, byte[] body, Instant expiresAt) {

  public CachedResponse {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(expiresAt, "expiresAt");
    body = body.clone();
  }

  @Override
  public byte[] body() {
    return body.clone();
  }
}
