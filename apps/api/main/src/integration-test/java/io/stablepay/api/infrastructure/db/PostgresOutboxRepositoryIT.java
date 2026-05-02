package io.stablepay.api.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Round-trips outbox publishes through a real Postgres container. Wires the adapter manually (no
 * Spring context) per the standards' integration-test pyramid level.
 */
@Testcontainers
class PostgresOutboxRepositoryIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("stablepay")
          .withUsername("stablepay")
          .withPassword("stablepay");

  private static HikariDataSource dataSource;
  private static NamedParameterJdbcTemplate jdbc;
  private static PostgresOutboxRepository repository;

  @BeforeAll
  static void setup() {
    dataSource = buildDataSource();
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    jdbc = new NamedParameterJdbcTemplate(dataSource);
    repository = new PostgresOutboxRepository(jdbc);
  }

  @AfterAll
  static void tearDown() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void publishIdempotent_twiceWithSameKeyAndTopic_yieldsSingleRow() {
    // given
    var key = "outbox-key-dup-" + java.util.UUID.randomUUID();
    var topic = "stablepay.test.events";
    var payload = "first-payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    repository.publishIdempotent(key, topic, payload);
    repository.publishIdempotent(
        key, topic, "second-payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    // when
    var rowCount = countRows(key, topic);

    // then
    assertThat(rowCount).isEqualTo(1L);
  }

  @Test
  void publishIdempotent_sameKeyDifferentTopic_yieldsTwoRows() {
    // given
    var key = "outbox-key-multi-topic-" + java.util.UUID.randomUUID();
    var topicA = "stablepay.test.topic-a";
    var topicB = "stablepay.test.topic-b";
    var payload = "payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    repository.publishIdempotent(key, topicA, payload);
    repository.publishIdempotent(key, topicB, payload);

    // when
    var rowCountA = countRows(key, topicA);
    var rowCountB = countRows(key, topicB);

    // then
    assertThat(rowCountA + rowCountB).isEqualTo(2L);
  }

  private static long countRows(String key, String topic) {
    var params = new MapSqlParameterSource().addValue("key", key).addValue("topic", topic);
    var result =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM namastack_outbox WHERE idempotency_key = :key AND event_topic"
                + " = :topic",
            params,
            Long.class);
    return result == null ? 0L : result;
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
