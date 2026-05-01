package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record RefreshToken(
    RefreshTokenId id,
    UserId userId,
    String tokenHash,
    Instant issuedAt,
    Instant expiresAt,
    Optional<Instant> revokedAt) {

  public boolean isActive(Instant now) {
    return revokedAt.isEmpty() && expiresAt.isAfter(now);
  }

  public RefreshToken revoke(Instant at) {
    return toBuilder().revokedAt(Optional.of(at)).build();
  }
}
