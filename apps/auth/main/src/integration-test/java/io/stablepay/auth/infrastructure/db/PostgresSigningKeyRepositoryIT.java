package io.stablepay.auth.infrastructure.db;

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

  @Test
  void saveThenFindActiveRoundTripsAllFields() {
    var repository = new PostgresSigningKeyRepository(namedJdbc);
    var key = signingKey("key-2026-05-01", true, NOW);
    repository.save(key);

    var actual = repository.findActive();

    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(key));
  }

  @Test
  void findActiveReturnsEmptyWhenNoActiveKey() {
    var repository = new PostgresSigningKeyRepository(namedJdbc);
    repository.save(signingKey("retired-key", false, NOW.minus(30, ChronoUnit.DAYS)));

    var actual = repository.findActive();

    assertThat(actual).isEmpty();
  }

  @Test
  void findAllReturnsKeysOrderedByCreatedAtDescending() {
    var repository = new PostgresSigningKeyRepository(namedJdbc);
    var older = signingKey("older-key", false, NOW.minus(2, ChronoUnit.DAYS));
    var newer = signingKey("newer-key", true, NOW);
    var oldest = signingKey("oldest-key", false, NOW.minus(10, ChronoUnit.DAYS));
    repository.save(older);
    repository.save(newer);
    repository.save(oldest);

    var actual = repository.findAll();

    assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(newer, older, oldest));
  }

  private static SigningKey signingKey(String kid, boolean isActive, Instant createdAt) {
    return SigningKey.builder()
        .kid(kid)
        .privateKeyPem("fake-private-pem-" + kid)
        .publicKeyPem("fake-public-pem-" + kid)
        .algorithm("RS256")
        .createdAt(createdAt)
        .isActive(isActive)
        .build();
  }
}
