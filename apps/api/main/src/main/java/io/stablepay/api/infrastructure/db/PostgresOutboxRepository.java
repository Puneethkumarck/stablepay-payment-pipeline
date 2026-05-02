package io.stablepay.api.infrastructure.db;

import io.stablepay.api.domain.port.OutboxRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Postgres-backed adapter for {@link OutboxRepository}. Inserts into the {@code namastack_outbox}
 * table maintained by Flyway migration {@code V2__SPP-88_outbox.sql}; the {@code
 * namastack-outbox-starter} publisher (wired in a later phase) drains unpublished rows to Kafka.
 *
 * <p>Idempotency is enforced by the partial unique index on {@code (idempotency_key, event_topic)}
 * (defined in V2). {@code ON CONFLICT DO NOTHING} silently absorbs duplicate inserts, which is the
 * intended happy path for client retries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PostgresOutboxRepository implements OutboxRepository {

  static final String SQL_PUBLISH_IDEMPOTENT =
      "INSERT INTO namastack_outbox (idempotency_key, event_topic, payload, created_at)"
          + " VALUES (:key, :topic, :payload, :now)"
          + " ON CONFLICT (idempotency_key, event_topic)"
          + " WHERE idempotency_key IS NOT NULL DO NOTHING";

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public void publishIdempotent(String idempotencyKey, String topic, byte[] payload) {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payload, "payload");
    var params =
        new MapSqlParameterSource()
            .addValue("key", idempotencyKey)
            .addValue("topic", topic)
            .addValue("payload", payload)
            .addValue("now", Timestamp.from(Instant.now()));
    try {
      jdbc.update(SQL_PUBLISH_IDEMPOTENT, params);
    } catch (DataAccessException e) {
      throw PostgresAdapterException.outbox(e);
    }
  }
}
