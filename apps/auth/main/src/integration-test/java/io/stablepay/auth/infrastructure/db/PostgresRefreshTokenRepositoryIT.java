package io.stablepay.auth.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.RefreshTokenId;
import io.stablepay.auth.domain.model.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresRefreshTokenRepositoryIT extends PostgresRepositoryIntegrationTest {

  private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final Instant NOW =
      Instant.parse("2026-05-01T10:00:00Z").truncatedTo(ChronoUnit.MILLIS);
  private static final Instant EXPIRES_AT = NOW.plus(7, ChronoUnit.DAYS);

  @Test
  void saveThenFindActiveByHashRoundTripsAllFields() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    var token = sampleToken("token-hash-active");
    repository.save(token);

    var actual = repository.findActiveByHash("token-hash-active", NOW);

    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(token));
  }

  @Test
  void findActiveByHashReturnsEmptyWhenTokenIsExpired() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    repository.save(sampleToken("token-hash-expired"));

    var actual = repository.findActiveByHash("token-hash-expired", EXPIRES_AT.plusSeconds(1));

    assertThat(actual).isEmpty();
  }

  @Test
  void findActiveByHashReturnsEmptyAfterRevoke() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    var token = sampleToken("token-hash-revoke");
    repository.save(token);

    repository.revoke(token.id(), NOW.plusSeconds(60));

    assertThat(repository.findActiveByHash("token-hash-revoke", NOW)).isEmpty();
  }

  @Test
  void revokeDoesNotOverwriteAlreadyRevokedTimestamp() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    var token = sampleToken("token-hash-double-revoke");
    repository.save(token);
    var firstRevocation = NOW.plusSeconds(60);
    var secondRevocation = NOW.plusSeconds(120);

    repository.revoke(token.id(), firstRevocation);
    repository.revoke(token.id(), secondRevocation);

    var storedRevokedAt =
        jdbc.queryForObject(
                "SELECT revoked_at FROM refresh_tokens WHERE token_id = ?",
                java.sql.Timestamp.class,
                token.id().value())
            .toInstant();
    assertThat(storedRevokedAt).isEqualTo(firstRevocation);
  }

  @Test
  void deleteRevokedAndExpiredOlderThanRemovesBothRevokedAndExpiredRows() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    var revokedToken =
        RefreshToken.builder()
            .id(RefreshTokenId.of(UUID.randomUUID()))
            .userId(UserId.of(ALICE_ID))
            .tokenHash("revoked")
            .issuedAt(NOW.minus(30, ChronoUnit.DAYS))
            .expiresAt(NOW.plus(1, ChronoUnit.DAYS))
            .revokedAt(Optional.of(NOW.minus(20, ChronoUnit.DAYS)))
            .build();
    var expiredToken =
        RefreshToken.builder()
            .id(RefreshTokenId.of(UUID.randomUUID()))
            .userId(UserId.of(ALICE_ID))
            .tokenHash("expired")
            .issuedAt(NOW.minus(40, ChronoUnit.DAYS))
            .expiresAt(NOW.minus(15, ChronoUnit.DAYS))
            .revokedAt(Optional.empty())
            .build();
    var keptToken = sampleToken("kept");
    repository.save(revokedToken);
    repository.save(expiredToken);
    repository.save(keptToken);

    var deleted = repository.deleteRevokedAndExpiredOlderThan(NOW.minus(7, ChronoUnit.DAYS));

    assertThat(deleted).isEqualTo(2);
    assertThat(repository.findActiveByHash("kept", NOW))
        .usingRecursiveComparison()
        .isEqualTo(Optional.of(keptToken));
  }

  @Test
  void partialUniqueIndexAllowsReusingTokenHashAfterRevocation() {
    var repository = new PostgresRefreshTokenRepository(namedJdbc);
    var first =
        RefreshToken.builder()
            .id(RefreshTokenId.of(UUID.randomUUID()))
            .userId(UserId.of(ALICE_ID))
            .tokenHash("reusable-hash")
            .issuedAt(NOW)
            .expiresAt(EXPIRES_AT)
            .revokedAt(Optional.empty())
            .build();
    repository.save(first);
    repository.revoke(first.id(), NOW.plusSeconds(60));

    var second =
        RefreshToken.builder()
            .id(RefreshTokenId.of(UUID.randomUUID()))
            .userId(UserId.of(ALICE_ID))
            .tokenHash("reusable-hash")
            .issuedAt(NOW.plusSeconds(120))
            .expiresAt(EXPIRES_AT.plusSeconds(120))
            .revokedAt(Optional.empty())
            .build();
    repository.save(second);

    assertThat(repository.findActiveByHash("reusable-hash", NOW))
        .usingRecursiveComparison()
        .isEqualTo(Optional.of(second));
  }

  private static RefreshToken sampleToken(String hash) {
    return RefreshToken.builder()
        .id(RefreshTokenId.of(UUID.randomUUID()))
        .userId(UserId.of(ALICE_ID))
        .tokenHash(hash)
        .issuedAt(NOW)
        .expiresAt(EXPIRES_AT)
        .revokedAt(Optional.empty())
        .build();
  }
}
