package io.stablepay.auth.application;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthApplicationConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
