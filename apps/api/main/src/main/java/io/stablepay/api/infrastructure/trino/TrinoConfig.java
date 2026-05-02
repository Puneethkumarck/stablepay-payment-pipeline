package io.stablepay.api.infrastructure.trino;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
@EnableConfigurationProperties(TrinoProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TrinoConfig {

  static final String TRINO_DRIVER_CLASS = "io.trino.jdbc.TrinoDriver";

  private final TrinoProperties properties;

  @Bean(name = "trinoDataSource")
  public DataSource trinoDataSource() {
    var config = new HikariConfig();
    config.setJdbcUrl(properties.url());
    config.setUsername(properties.user());
    config.setDriverClassName(TRINO_DRIVER_CLASS);
    config.setAutoCommit(true);
    config.setMaximumPoolSize(8);
    config.setPoolName("stablepay-trino");
    log.info("Configured Trino DataSource at {}", properties.url());
    return new HikariDataSource(config);
  }

  @Bean(name = "trinoJdbcTemplate")
  public NamedParameterJdbcTemplate trinoJdbcTemplate(
      @Qualifier("trinoDataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }
}
