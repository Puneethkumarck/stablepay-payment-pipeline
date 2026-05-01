package io.stablepay.auth.infrastructure.db;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.refreshTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresRefreshTokenRepositoryIT extends PostgresRepositoryIntegrationTest {

  private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UserId ALICE_USER_ID = UserId.of(ALICE_ID);
  private static final Instant NOW =
      Instant.parse("2026-05-01T10:00:00Z").truncatedTo(ChronoUnit.MILLIS);
  private static final Instant EXPIRES_AT = NOW.plus(7, ChronoUnit.DAYS);
  private static final RefreshTokenRowMapper TOKEN_ROW_MAPPER = new RefreshTokenRowMapperImpl();

  @Test
  void saveThenFindActiveByHashRoundTripsAllFields() {
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    var token = activeAliceToken("token-hash-active");
    repository.save(token);

    // when
    var actual = repository.findActiveByHash("token-hash-active", NOW);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(token));
  }

  @Test
  void findActiveByHashReturnsEmptyWhenTokenIsExpired() {
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    repository.save(activeAliceToken("token-hash-expired"));

    // when
    var actual = repository.findActiveByHash("token-hash-expired", EXPIRES_AT.plusSeconds(1));

    // then
    assertThat(actual).isEmpty();
  }

  @Test
  void findActiveByHashReturnsEmptyAfterRevoke() {
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    var token = activeAliceToken("token-hash-revoke");
    repository.save(token);
    repository.revoke(token.id(), NOW.plusSeconds(60));

    // when
    var actual = repository.findActiveByHash("token-hash-revoke", NOW);

    // then
    assertThat(actual).isEmpty();
  }

  @Test
  void revokeDoesNotOverwriteAlreadyRevokedTimestamp() {
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    var token = activeAliceToken("token-hash-double-revoke");
    var firstRevocation = NOW.plusSeconds(60);
    var secondRevocation = NOW.plusSeconds(120);
    repository.save(token);
    repository.revoke(token.id(), firstRevocation);

    // when
    repository.revoke(token.id(), secondRevocation);

    // then
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
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    var revokedToken =
        refreshTokenBuilder()
            .userId(ALICE_USER_ID)
            .tokenHash("revoked")
            .issuedAt(NOW.minus(30, ChronoUnit.DAYS))
            .expiresAt(NOW.plus(1, ChronoUnit.DAYS))
            .revokedAt(Optional.of(NOW.minus(20, ChronoUnit.DAYS)))
            .build();
    var expiredToken =
        refreshTokenBuilder()
            .userId(ALICE_USER_ID)
            .tokenHash("expired")
            .issuedAt(NOW.minus(40, ChronoUnit.DAYS))
            .expiresAt(NOW.minus(15, ChronoUnit.DAYS))
            .build();
    var keptToken = activeAliceToken("kept");
    repository.save(revokedToken);
    repository.save(expiredToken);
    repository.save(keptToken);

    // when
    var deleted = repository.deleteRevokedAndExpiredOlderThan(NOW.minus(7, ChronoUnit.DAYS));

    // then
    assertThat(deleted).isEqualTo(2);
    assertThat(repository.findActiveByHash("kept", NOW))
        .usingRecursiveComparison()
        .isEqualTo(Optional.of(keptToken));
  }

  @Test
  void partialUniqueIndexAllowsReusingTokenHashAfterRevocation() {
    // given
    var repository = new PostgresRefreshTokenRepository(namedJdbc, TOKEN_ROW_MAPPER);
    var first = activeAliceToken("reusable-hash");
    repository.save(first);
    repository.revoke(first.id(), NOW.plusSeconds(60));
    var second =
        refreshTokenBuilder()
            .userId(ALICE_USER_ID)
            .tokenHash("reusable-hash")
            .issuedAt(NOW.plusSeconds(120))
            .expiresAt(EXPIRES_AT.plusSeconds(120))
            .build();

    // when
    repository.save(second);

    // then
    assertThat(repository.findActiveByHash("reusable-hash", NOW))
        .usingRecursiveComparison()
        .isEqualTo(Optional.of(second));
  }

  private static RefreshToken activeAliceToken(String tokenHash) {
    return refreshTokenBuilder()
        .userId(ALICE_USER_ID)
        .tokenHash(tokenHash)
        .issuedAt(NOW)
        .expiresAt(EXPIRES_AT)
        .build();
  }
}
