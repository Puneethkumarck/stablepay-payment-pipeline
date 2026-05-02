package io.stablepay.api.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PostgresIdempotencyRepositoryIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("stablepay")
          .withUsername("stablepay")
          .withPassword("stablepay");

  private static HikariDataSource dataSource;
  private static NamedParameterJdbcTemplate jdbc;
  private static PostgresIdempotencyRepository repository;

  @BeforeAll
  static void setup() {
    dataSource = buildDataSource();
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    jdbc = new NamedParameterJdbcTemplate(dataSource);
    repository = new PostgresIdempotencyRepository(jdbc);
  }

  @AfterAll
  static void tearDown() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void shouldReturnCachedResponseAfterSave() {
    // given
    var userId = UserId.of(UUID.randomUUID());
    var key = "key-save-then-find-" + UUID.randomUUID();
    var bodyBytes = new byte[] {1, 2, 3, 4, 5};
    var expiresAt = Instant.now().plusSeconds(3600);
    var cached = CachedResponse.builder().status(201).body(bodyBytes).expiresAt(expiresAt).build();
    var expected =
        CachedResponse.builder()
            .status(201)
            .body(new byte[] {1, 2, 3, 4, 5})
            .expiresAt(expiresAt)
            .build();
    repository.tryAcquire(key, userId, expiresAt);
    repository.save(key, userId, cached);

    // when
    var actual = repository.findActive(key, userId, Instant.now());

    // then
    assertThat(actual)
        .isPresent()
        .get()
        .usingRecursiveComparison()
        .ignoringFields("expiresAt")
        .isEqualTo(expected);
  }

  @Test
  void shouldReturnEmptyWhenExpiresAtInPast() {
    // given
    var userId = UserId.of(UUID.randomUUID());
    var key = "key-expired-" + UUID.randomUUID();
    var pastExpiry = Instant.now().minusSeconds(60);
    var cached =
        CachedResponse.builder().status(200).body(new byte[] {9}).expiresAt(pastExpiry).build();
    repository.tryAcquire(key, userId, pastExpiry);
    repository.save(key, userId, cached);
    var expected = Optional.<CachedResponse>empty();

    // when
    var actual = repository.findActive(key, userId, Instant.now());

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldAbsorbSecondInsertOnSameKey() {
    // given
    var userId = UserId.of(UUID.randomUUID());
    var key = "key-conflict-" + UUID.randomUUID();
    var firstExpiresAt = Instant.now().plusSeconds(3600);
    var firstBody = new byte[] {1, 1, 1};
    var first =
        CachedResponse.builder().status(200).body(firstBody).expiresAt(firstExpiresAt).build();
    repository.tryAcquire(key, userId, firstExpiresAt);
    repository.save(key, userId, first);
    var secondAcquired = repository.tryAcquire(key, userId, firstExpiresAt.plusSeconds(60));
    var expected =
        CachedResponse.builder()
            .status(200)
            .body(new byte[] {1, 1, 1})
            .expiresAt(firstExpiresAt)
            .build();

    // when
    var actual = repository.findActive(key, userId, Instant.now());

    // then
    assertThat(secondAcquired).isFalse();
    assertThat(actual)
        .isPresent()
        .get()
        .usingRecursiveComparison()
        .ignoringFields("expiresAt")
        .isEqualTo(expected);
  }

  @Test
  void shouldDeleteOnlyExpiredRowsAndReturnCount() {
    // given
    var userId = UserId.of(UUID.randomUUID());
    var expiredKey = "key-cleanup-expired-" + UUID.randomUUID();
    var freshKey = "key-cleanup-fresh-" + UUID.randomUUID();
    var pastExpiry = Instant.now().minusSeconds(120);
    var futureExpiry = Instant.now().plusSeconds(3600);
    repository.tryAcquire(expiredKey, userId, pastExpiry);
    repository.save(
        expiredKey,
        userId,
        CachedResponse.builder().status(200).body(new byte[] {1}).expiresAt(pastExpiry).build());
    repository.tryAcquire(freshKey, userId, futureExpiry);
    repository.save(
        freshKey,
        userId,
        CachedResponse.builder().status(200).body(new byte[] {2}).expiresAt(futureExpiry).build());

    // when
    var deletedCount = repository.deleteExpired(Instant.now());

    // then
    assertThat(deletedCount).isGreaterThanOrEqualTo(1);
    assertThat(repository.findActive(expiredKey, userId, Instant.now())).isEmpty();
    assertThat(repository.findActive(freshKey, userId, Instant.now())).isPresent();
  }

  private static HikariDataSource buildDataSource() {
    var config = new HikariConfig();
    config.setJdbcUrl(POSTGRES.getJdbcUrl());
    config.setUsername(POSTGRES.getUsername());
    config.setPassword(POSTGRES.getPassword());
    config.setMaximumPoolSize(4);
    return new HikariDataSource(config);
  }
}
