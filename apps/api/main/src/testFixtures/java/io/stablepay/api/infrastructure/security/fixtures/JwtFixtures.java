package io.stablepay.api.infrastructure.security.fixtures;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtFixtures {

  public static final String SOME_JWT_TOKEN_VALUE = "token-value";
  public static final String SOME_JWT_KID = "test-key-1";
  public static final String SOME_JWT_ALG = "RS256";
  public static final Instant SOME_JWT_ISSUED_AT = Instant.parse("2026-05-01T10:00:00Z");
  public static final Instant SOME_JWT_EXPIRES_AT = Instant.parse("2026-05-01T10:15:00Z");
  public static final String SOME_JWT_ISSUER = "https://auth.stablepay.local";
  public static final List<String> SOME_JWT_AUDIENCE = List.of("stablepay-api");

  private JwtFixtures() {}

  public static Jwt.Builder jwtBuilder() {
    return Jwt.withTokenValue(SOME_JWT_TOKEN_VALUE)
        .header("alg", SOME_JWT_ALG)
        .header("kid", SOME_JWT_KID)
        .issuedAt(SOME_JWT_ISSUED_AT)
        .expiresAt(SOME_JWT_EXPIRES_AT)
        .issuer(SOME_JWT_ISSUER)
        .audience(SOME_JWT_AUDIENCE);
  }
}
