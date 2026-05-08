package io.stablepay.api.config;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.stablepay.api.infrastructure.ratelimit.RateLimitFilter;
import io.stablepay.api.infrastructure.security.JwtToAuthenticatedUserConverter;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    classes = BusinessTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("business-test")
@Import({StubRepositoryConfig.class, BusinessTestBase.JwtConfig.class})
public abstract class BusinessTestBase {

  static final UUID BOB_USER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  static final UUID BOB_CUSTOMER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  static final String BOB_EMAIL = "bob@stablepay.io";

  private static final RSAKey RSA_KEY;

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("stablepay")
          .withUsername("stablepay")
          .withPassword("stablepay");

  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8.0-alpine").withExposedPorts(6379);

  static {
    try {
      RSA_KEY = new RSAKeyGenerator(2048).keyID("test-kid").generate();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate RSA key pair", e);
    }
    POSTGRES.start();
    REDIS.start();
  }

  @LocalServerPort protected int port;

  @Autowired protected StubRepositoryConfig stubRepositoryConfig;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "stablepay.ratelimit.redis-uri",
        () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  protected static String aliceJwt() {
    return mintJwt(
        SOME_CUSTOMER_USER_UUID,
        SOME_CUSTOMER_EMAIL,
        List.of("CUSTOMER"),
        SOME_CUSTOMER_UUID.toString());
  }

  protected static String bobJwt() {
    return mintJwt(BOB_USER_UUID, BOB_EMAIL, List.of("CUSTOMER"), BOB_CUSTOMER_UUID.toString());
  }

  protected static String adminJwt() {
    return mintJwt(SOME_ADMIN_USER_UUID, SOME_ADMIN_EMAIL, List.of("ADMIN"), null);
  }

  protected RestClient authenticatedClient(String jwt) {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeader("Authorization", "Bearer " + jwt)
        .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory())
        .build();
  }

  private static String mintJwt(UUID userId, String email, List<String> roles, String customerId) {
    try {
      var claimsBuilder =
          new JWTClaimsSet.Builder()
              .subject(userId.toString())
              .claim("email", email)
              .claim("roles", roles)
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
      if (customerId != null) {
        claimsBuilder.claim("customer_id", customerId);
      }
      var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_KEY.getKeyID()).build();
      var signedJwt = new SignedJWT(header, claimsBuilder.build());
      signedJwt.sign(new RSASSASigner(RSA_KEY));
      return signedJwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to mint JWT", e);
    }
  }

  @org.springframework.boot.test.context.TestConfiguration
  @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(
      securedEnabled = true)
  @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
  static class JwtConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
      "/actuator/health", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**"
    };

    @org.springframework.context.annotation.Bean
    public JwtDecoder jwtDecoder() {
      try {
        return NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to create JwtDecoder", e);
      }
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    public SecurityFilterChain testSecurityFilterChain(
        HttpSecurity http,
        JwtToAuthenticatedUserConverter jwtConverter,
        RateLimitFilter rateLimitFilter,
        JwtDecoder jwtDecoder)
        throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              a -> a.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated())
          .oauth2ResourceServer(
              o -> o.jwt(j -> j.decoder(jwtDecoder).jwtAuthenticationConverter(jwtConverter)))
          .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
          .build();
    }
  }
}
