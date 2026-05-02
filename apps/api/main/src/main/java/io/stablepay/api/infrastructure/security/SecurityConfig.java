package io.stablepay.api.infrastructure.security;

import io.stablepay.api.infrastructure.ratelimit.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
    "/actuator/health", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**"
  };

  @Bean
  public SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      JwtToAuthenticatedUserConverter jwtConverter,
      RateLimitFilter rateLimitFilter,
      @Value("${stablepay.auth.jwks-uri}") String jwksUri)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> a.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            o -> o.jwt(j -> j.jwkSetUri(jwksUri).jwtAuthenticationConverter(jwtConverter)))
        .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
        .build();
  }
}
