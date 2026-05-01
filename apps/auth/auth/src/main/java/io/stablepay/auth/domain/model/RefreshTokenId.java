package io.stablepay.auth.domain.model;

import java.util.UUID;

public record RefreshTokenId(UUID value) {

  public static RefreshTokenId of(UUID value) {
    return new RefreshTokenId(value);
  }
}
