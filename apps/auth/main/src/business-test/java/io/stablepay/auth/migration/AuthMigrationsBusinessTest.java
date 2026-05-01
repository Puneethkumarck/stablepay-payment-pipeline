package io.stablepay.auth.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AuthMigrationsBusinessTest {

  private static final String PLAIN_PASSWORD = "demo1234";
  private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID AGENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("stablepay_auth")
          .withUsername("stablepay")
          .withPassword("stablepay");

  private static JdbcTemplate jdbc;

  @BeforeAll
  static void migrate() {
    var dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    jdbc = new JdbcTemplate(dataSource);
  }

  @Test
  void v1SeedsFourUsersWithBcryptHashesMatchingDemo1234() {
    var encoder = new BCryptPasswordEncoder();

    var actual =
        jdbc.query(
            "SELECT user_id, customer_id, email, roles, password FROM users ORDER BY email",
            (rs, rowNum) ->
                new SeededUser(
                    rs.getObject("user_id", UUID.class),
                    rs.getObject("customer_id", UUID.class),
                    rs.getString("email"),
                    rs.getString("roles"),
                    encoder.matches(PLAIN_PASSWORD, rs.getString("password"))));

    var expected =
        List.of(
            new SeededUser(ADMIN_ID, null, "admin@stablepay.io", "ADMIN", true),
            new SeededUser(AGENT_ID, null, "agent@stablepay.io", "AGENT", true),
            new SeededUser(ALICE_ID, ALICE_ID, "alice@stablepay.io", "ADMIN,CUSTOMER", true),
            new SeededUser(BOB_ID, BOB_ID, "bob@stablepay.io", "CUSTOMER", true));

    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void v2RefreshTokensRejectsDuplicateActiveTokenHash() {
    var hash = "duplicate-active-hash";
    insertActiveRefreshToken(hash);

    assertThatThrownBy(() -> insertActiveRefreshToken(hash))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void v2RefreshTokensAllowsReuseOfTokenHashAfterRevocation() {
    var hash = "revoke-then-reuse-hash";
    var firstId = insertActiveRefreshToken(hash);
    jdbc.update("UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_id = ?", firstId);

    insertActiveRefreshToken(hash);

    var activeCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ? AND revoked_at IS NULL",
            Integer.class,
            hash);
    assertThat(activeCount).isOne();
  }

  @Test
  void v3JwtSigningKeysAppliesRs256AndIsActiveDefaults() {
    jdbc.update(
        "INSERT INTO jwt_signing_keys (kid, private_key_pem, public_key_pem) VALUES (?, ?, ?)",
        "test-kid",
        "test-private-pem",
        "test-public-pem");

    var actual =
        jdbc.queryForObject(
            "SELECT kid, private_key_pem, public_key_pem, algorithm, is_active FROM jwt_signing_keys WHERE kid = ?",
            (rs, rowNum) ->
                new SigningKey(
                    rs.getString("kid"),
                    rs.getString("private_key_pem"),
                    rs.getString("public_key_pem"),
                    rs.getString("algorithm"),
                    rs.getBoolean("is_active")),
            "test-kid");

    var expected = new SigningKey("test-kid", "test-private-pem", "test-public-pem", "RS256", true);

    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  private UUID insertActiveRefreshToken(String tokenHash) {
    var tokenId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO refresh_tokens (token_id, user_id, token_hash, expires_at) VALUES (?, ?, ?, NOW() + INTERVAL '1 hour')",
        tokenId,
        ALICE_ID,
        tokenHash);
    return tokenId;
  }

  private record SeededUser(
      UUID userId, UUID customerId, String email, String roles, boolean passwordMatchesDemo1234) {}

  private record SigningKey(
      String kid, String privateKeyPem, String publicKeyPem, String algorithm, boolean isActive) {}
}
