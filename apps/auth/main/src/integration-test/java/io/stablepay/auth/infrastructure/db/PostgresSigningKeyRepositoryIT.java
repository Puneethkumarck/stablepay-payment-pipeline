package io.stablepay.auth.infrastructure.db;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.signingKeyBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.auth.domain.model.SigningKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PostgresSigningKeyRepositoryIT extends PostgresRepositoryIntegrationTest {

  private static final Instant NOW =
      Instant.parse("2026-05-01T10:00:00Z").truncatedTo(ChronoUnit.MILLIS);
  private static final SigningKeyRowMapper KEY_ROW_MAPPER = new SigningKeyRowMapperImpl();

  @Test
  void saveThenFindActiveRoundTripsAllFields() {
    // given
    var repository = new PostgresSigningKeyRepository(namedJdbc, KEY_ROW_MAPPER);
    var key = signingKey("key-2026-05-01", true, NOW);
    repository.save(key);

    // when
    var actual = repository.findActive();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(key));
  }

  @Test
  void findActiveReturnsEmptyWhenNoActiveKey() {
    // given
    var repository = new PostgresSigningKeyRepository(namedJdbc, KEY_ROW_MAPPER);
    repository.save(signingKey("retired-key", false, NOW.minus(30, ChronoUnit.DAYS)));

    // when
    var actual = repository.findActive();

    // then
    assertThat(actual).isEmpty();
  }

  @Test
  void findAllReturnsKeysOrderedByCreatedAtDescending() {
    // given
    var repository = new PostgresSigningKeyRepository(namedJdbc, KEY_ROW_MAPPER);
    var older = signingKey("older-key", false, NOW.minus(2, ChronoUnit.DAYS));
    var newer = signingKey("newer-key", true, NOW);
    var oldest = signingKey("oldest-key", false, NOW.minus(10, ChronoUnit.DAYS));
    repository.save(older);
    repository.save(newer);
    repository.save(oldest);

    // when
    var actual = repository.findAll();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(newer, older, oldest));
  }

  private static SigningKey signingKey(String kid, boolean isActive, Instant createdAt) {
    return signingKeyBuilder()
        .kid(kid)
        .privateKeyPem("fake-private-pem-" + kid)
        .publicKeyPem("fake-public-pem-" + kid)
        .createdAt(createdAt)
        .isActive(isActive)
        .build();
  }
}
