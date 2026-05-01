package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.CustomerId;
import io.stablepay.auth.domain.model.Role;
import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.model.UserId;
import io.stablepay.auth.domain.port.UserRepository;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresUserRepository implements UserRepository {

  private static final String SELECT_COLUMNS =
      "user_id, customer_id, email, password, roles, created_at, updated_at";

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<User> findByEmail(String email) {
    var rows =
        jdbc.queryForList(
            "SELECT " + SELECT_COLUMNS + " FROM users WHERE LOWER(email) = LOWER(:email)",
            new MapSqlParameterSource("email", email));
    return rows.stream().findFirst().map(PostgresUserRepository::toDomain);
  }

  @Override
  public Optional<User> findById(UserId id) {
    var rows =
        jdbc.queryForList(
            "SELECT " + SELECT_COLUMNS + " FROM users WHERE user_id = :id",
            new MapSqlParameterSource("id", id.value()));
    return rows.stream().findFirst().map(PostgresUserRepository::toDomain);
  }

  private static User toDomain(Map<String, Object> row) {
    return User.builder()
        .id(UserId.of((UUID) row.get("user_id")))
        .customerId(Optional.ofNullable((UUID) row.get("customer_id")).map(CustomerId::of))
        .email((String) row.get("email"))
        .passwordHash((String) row.get("password"))
        .roles(parseRoles((String) row.get("roles")))
        .createdAt(((Timestamp) row.get("created_at")).toInstant())
        .updatedAt(((Timestamp) row.get("updated_at")).toInstant())
        .build();
  }

  private static Set<Role> parseRoles(String csv) {
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Role::valueOf)
        .collect(Collectors.toUnmodifiableSet());
  }
}
