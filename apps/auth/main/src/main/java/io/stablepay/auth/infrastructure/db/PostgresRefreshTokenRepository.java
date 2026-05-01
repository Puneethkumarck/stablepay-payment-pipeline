package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.RefreshTokenId;
import io.stablepay.auth.domain.port.RefreshTokenRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresRefreshTokenRepository implements RefreshTokenRepository {

  private static final String SELECT_COLUMNS =
      "token_id, user_id, token_hash, issued_at, expires_at, revoked_at";

  private static final RowMapper<RefreshTokenRow> ROW_MAPPER =
      new DataClassRowMapper<>(RefreshTokenRow.class);

  private final NamedParameterJdbcTemplate jdbc;
  private final RefreshTokenRowMapper mapper;

  @Override
  public void save(RefreshToken token) {
    var params =
        new MapSqlParameterSource()
            .addValue("tokenId", token.id().value())
            .addValue("userId", token.userId().value())
            .addValue("tokenHash", token.tokenHash())
            .addValue("issuedAt", Timestamp.from(token.issuedAt()))
            .addValue("expiresAt", Timestamp.from(token.expiresAt()))
            .addValue("revokedAt", token.revokedAt().map(Timestamp::from).orElse(null));
    jdbc.update(
        "INSERT INTO refresh_tokens (token_id, user_id, token_hash, issued_at, expires_at, revoked_at) "
            + "VALUES (:tokenId, :userId, :tokenHash, :issuedAt, :expiresAt, :revokedAt)",
        params);
  }

  @Override
  public Optional<RefreshToken> findActiveByHash(String tokenHash, Instant now) {
    var params =
        new MapSqlParameterSource()
            .addValue("tokenHash", tokenHash)
            .addValue("now", Timestamp.from(now));
    return jdbc
        .query(
            "SELECT "
                + SELECT_COLUMNS
                + " FROM refresh_tokens "
                + "WHERE token_hash = :tokenHash AND expires_at > :now AND revoked_at IS NULL",
            params,
            ROW_MAPPER)
        .stream()
        .findFirst()
        .map(mapper::toDomain);
  }

  @Override
  public void revoke(RefreshTokenId id, Instant at) {
    var params =
        new MapSqlParameterSource().addValue("id", id.value()).addValue("at", Timestamp.from(at));
    jdbc.update(
        "UPDATE refresh_tokens SET revoked_at = :at WHERE token_id = :id AND revoked_at IS NULL",
        params);
  }

  @Override
  public int deleteRevokedAndExpiredOlderThan(Instant cutoff) {
    var params = new MapSqlParameterSource("cutoff", Timestamp.from(cutoff));
    return jdbc.update(
        "DELETE FROM refresh_tokens WHERE revoked_at < :cutoff OR expires_at < :cutoff", params);
  }
}
