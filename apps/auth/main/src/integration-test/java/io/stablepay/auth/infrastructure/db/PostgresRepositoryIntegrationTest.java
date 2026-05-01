package io.stablepay.auth.infrastructure.db;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
abstract class PostgresRepositoryIntegrationTest {

  @Container
  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("stablepay_auth")
          .withUsername("stablepay")
          .withPassword("stablepay");

  protected static DataSource dataSource;
  protected static JdbcTemplate jdbc;
  protected static NamedParameterJdbcTemplate namedJdbc;

  @BeforeAll
  static void migrate() {
    dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    jdbc = new JdbcTemplate(dataSource);
    namedJdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  @BeforeEach
  void cleanRefreshTokensAndSigningKeys() {
    jdbc.update("DELETE FROM refresh_tokens");
    jdbc.update("DELETE FROM jwt_signing_keys");
  }
}
