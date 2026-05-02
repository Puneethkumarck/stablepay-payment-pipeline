package io.stablepay.api.infrastructure.db;

import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.model.UserId;
import io.stablepay.api.domain.port.IdempotencyRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostgresIdempotencyRepository implements IdempotencyRepository {

  static final String SQL_FIND_ACTIVE =
      "SELECT response_status, response_body, expires_at"
          + " FROM idempotency_keys"
          + " WHERE idempotency_key = :key AND user_id = :userId AND expires_at > :now";

  static final String SQL_SAVE =
      "INSERT INTO idempotency_keys (idempotency_key, user_id, response_status, response_body,"
          + " expires_at)"
          + " VALUES (:key, :userId, :status, :body, :expiresAt)"
          + " ON CONFLICT (idempotency_key, user_id) DO NOTHING";

  static final String SQL_DELETE_EXPIRED = "DELETE FROM idempotency_keys WHERE expires_at <= :now";

  static final RowMapper<CachedResponse> CACHED_RESPONSE_ROW_MAPPER =
      (rs, rowNum) ->
          new CachedResponse(
              rs.getInt("response_status"),
              Optional.ofNullable(rs.getBytes("response_body")).orElseGet(() -> new byte[0]),
              rs.getTimestamp("expires_at").toInstant());

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<CachedResponse> findActive(String key, UserId userId, Instant now) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(now, "now");
    var params =
        new MapSqlParameterSource()
            .addValue("key", key)
            .addValue("userId", userId.value())
            .addValue("now", Timestamp.from(now));
    try {
      return jdbc.query(SQL_FIND_ACTIVE, params, CACHED_RESPONSE_ROW_MAPPER).stream().findFirst();
    } catch (DataAccessException e) {
      throw PostgresAdapterException.idempotency(e);
    }
  }

  @Override
  public void save(String key, UserId userId, CachedResponse response) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(response, "response");
    var body = response.body();
    var params =
        new MapSqlParameterSource()
            .addValue("key", key)
            .addValue("userId", userId.value())
            .addValue("status", response.status())
            .addValue("body", body)
            .addValue("expiresAt", Timestamp.from(response.expiresAt()));
    try {
      jdbc.update(SQL_SAVE, params);
    } catch (DataAccessException e) {
      throw PostgresAdapterException.idempotency(e);
    }
  }

  @Override
  public int deleteExpired(Instant now) {
    Objects.requireNonNull(now, "now");
    var params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
    try {
      return jdbc.update(SQL_DELETE_EXPIRED, params);
    } catch (DataAccessException e) {
      throw PostgresAdapterException.idempotency(e);
    }
  }
}
