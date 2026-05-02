package io.stablepay.auth.infrastructure.security;

import io.stablepay.auth.infrastructure.ratelimit.LoginRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final LoginRateLimitFilter loginRateLimitFilter;

  @Bean
  public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/.well-known/jwks.json",
                        "/actuator/health",
                        "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration(
      LoginRateLimitFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
