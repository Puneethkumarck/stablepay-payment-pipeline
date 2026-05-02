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
    var cached = new CachedResponse(201, bodyBytes, expiresAt);
    var expected = new CachedResponse(201, new byte[] {1, 2, 3, 4, 5}, expiresAt);
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
    var cached = new CachedResponse(200, new byte[] {9}, pastExpiry);
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
    var first = new CachedResponse(200, firstBody, firstExpiresAt);
    repository.save(key, userId, first);
    var secondBody = new byte[] {9, 9, 9};
    var second = new CachedResponse(500, secondBody, firstExpiresAt.plusSeconds(60));
    repository.save(key, userId, second);
    var expected = new CachedResponse(200, new byte[] {1, 1, 1}, firstExpiresAt);

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
  void shouldDeleteOnlyExpiredRowsAndReturnCount() {
    // given
    var userId = UserId.of(UUID.randomUUID());
    var expiredKey = "key-cleanup-expired-" + UUID.randomUUID();
    var freshKey = "key-cleanup-fresh-" + UUID.randomUUID();
    var pastExpiry = Instant.now().minusSeconds(120);
    var futureExpiry = Instant.now().plusSeconds(3600);
    repository.save(expiredKey, userId, new CachedResponse(200, new byte[] {1}, pastExpiry));
    repository.save(freshKey, userId, new CachedResponse(200, new byte[] {2}, futureExpiry));

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
