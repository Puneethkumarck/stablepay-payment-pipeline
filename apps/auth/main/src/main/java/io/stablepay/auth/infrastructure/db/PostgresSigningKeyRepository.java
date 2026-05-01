package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.SigningKeyRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresSigningKeyRepository implements SigningKeyRepository {

  private static final String SELECT_COLUMNS =
      "kid, private_key_pem, public_key_pem, algorithm, created_at, is_active";

  private static final RowMapper<SigningKeyRow> ROW_MAPPER =
      new DataClassRowMapper<>(SigningKeyRow.class);

  private final NamedParameterJdbcTemplate jdbc;
  private final SigningKeyRowMapper mapper;

  @Override
  public Optional<SigningKey> findActive() {
    return jdbc
        .query(
            "SELECT " + SELECT_COLUMNS + " FROM jwt_signing_keys WHERE is_active = TRUE LIMIT 1",
            new MapSqlParameterSource(),
            ROW_MAPPER)
        .stream()
        .findFirst()
        .map(mapper::toDomain);
  }

  @Override
  public List<SigningKey> findAll() {
    return jdbc
        .query(
            "SELECT " + SELECT_COLUMNS + " FROM jwt_signing_keys ORDER BY created_at DESC",
            new MapSqlParameterSource(),
            ROW_MAPPER)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public void save(SigningKey key) {
    var params =
        new MapSqlParameterSource()
            .addValue("kid", key.kid())
            .addValue("privateKeyPem", key.privateKeyPem())
            .addValue("publicKeyPem", key.publicKeyPem())
            .addValue("algorithm", key.algorithm())
            .addValue("createdAt", Timestamp.from(key.createdAt()))
            .addValue("isActive", key.isActive());
    jdbc.update(
        "INSERT INTO jwt_signing_keys (kid, private_key_pem, public_key_pem, algorithm, created_at, is_active) "
            + "VALUES (:kid, :privateKeyPem, :publicKeyPem, :algorithm, :createdAt, :isActive)",
        params);
  }
}
