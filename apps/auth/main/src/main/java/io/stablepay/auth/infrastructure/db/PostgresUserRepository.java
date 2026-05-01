package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.model.UserId;
import io.stablepay.auth.domain.port.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresUserRepository implements UserRepository {

  private static final String SELECT_COLUMNS =
      "user_id, customer_id, email, password, roles, created_at, updated_at";

  private static final RowMapper<UserRow> ROW_MAPPER = new DataClassRowMapper<>(UserRow.class);

  private final NamedParameterJdbcTemplate jdbc;
  private final UserRowMapper mapper;

  @Override
  public Optional<User> findByEmail(String email) {
    return jdbc
        .query(
            "SELECT " + SELECT_COLUMNS + " FROM users WHERE LOWER(email) = LOWER(:email)",
            new MapSqlParameterSource("email", email),
            ROW_MAPPER)
        .stream()
        .findFirst()
        .map(mapper::toDomain);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return jdbc
        .query(
            "SELECT " + SELECT_COLUMNS + " FROM users WHERE user_id = :id",
            new MapSqlParameterSource("id", id.value()),
            ROW_MAPPER)
        .stream()
        .findFirst()
        .map(mapper::toDomain);
  }
}
