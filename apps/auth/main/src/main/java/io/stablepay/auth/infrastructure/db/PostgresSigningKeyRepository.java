package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.SigningKeyRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresSigningKeyRepository implements SigningKeyRepository {

  private static final String SELECT_COLUMNS =
      "kid, private_key_pem, public_key_pem, algorithm, created_at, is_active";

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<SigningKey> findActive() {
    var rows =
        jdbc.queryForList(
            "SELECT " + SELECT_COLUMNS + " FROM jwt_signing_keys WHERE is_active = TRUE LIMIT 1",
            new MapSqlParameterSource());
    return rows.stream().findFirst().map(PostgresSigningKeyRepository::toDomain);
  }

  @Override
  public List<SigningKey> findAll() {
    var rows =
        jdbc.queryForList(
            "SELECT " + SELECT_COLUMNS + " FROM jwt_signing_keys ORDER BY created_at DESC",
            new MapSqlParameterSource());
    return rows.stream().map(PostgresSigningKeyRepository::toDomain).toList();
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

  private static SigningKey toDomain(Map<String, Object> row) {
    return SigningKey.builder()
        .kid((String) row.get("kid"))
        .privateKeyPem((String) row.get("private_key_pem"))
        .publicKeyPem((String) row.get("public_key_pem"))
        .algorithm((String) row.get("algorithm"))
        .createdAt(((Timestamp) row.get("created_at")).toInstant())
        .isActive((Boolean) row.get("is_active"))
        .build();
  }
}
