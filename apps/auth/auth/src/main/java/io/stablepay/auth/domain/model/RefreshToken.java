package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
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

  public RefreshToken {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(issuedAt, "issuedAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(revokedAt, "revokedAt");
  }

  public boolean isActive(Instant now) {
    return revokedAt.isEmpty() && expiresAt.isAfter(now);
  }

  public RefreshToken revoke(Instant at) {
    return toBuilder().revokedAt(Optional.of(at)).build();
  }
}
