package io.stablepay.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class TestConfig {

  static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");

  @Bean
  Clock clock() {
    return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
  }

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
