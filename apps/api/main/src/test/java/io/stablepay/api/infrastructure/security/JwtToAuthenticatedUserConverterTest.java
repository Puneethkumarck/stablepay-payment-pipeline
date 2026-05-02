package io.stablepay.api.infrastructure.security;

import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAgentUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtToAuthenticatedUserConverterTest {

  private static final Instant ISSUED_AT = Instant.parse("2026-05-01T10:00:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2026-05-01T10:15:00Z");

  private final JwtToAuthenticatedUserConverter converter = new JwtToAuthenticatedUserConverter();

  @Test
  void shouldConvertCustomerJwtWithCustomerIdClaim() {
    var jwt =
        jwtBuilder()
            .subject(SOME_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    var result = converter.convert(jwt);

    var expected = someCustomerUser();
    assertThat(result).isInstanceOf(AuthenticatedUserToken.class);
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  @Test
  void shouldConvertAdminJwtWithoutCustomerIdClaimWithoutNpe() {
    var jwt =
        jwtBuilder()
            .subject(SOME_USER_UUID.toString())
            .claim("email", SOME_ADMIN_EMAIL)
            .claim("roles", List.of("ADMIN"))
            .build();

    var result = converter.convert(jwt);

    var expected = someAdminUser();
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  void shouldConvertAgentJwtWithoutCustomerIdClaimWithoutNpe() {
    var jwt =
        jwtBuilder()
            .subject(SOME_USER_UUID.toString())
            .claim("email", SOME_AGENT_EMAIL)
            .claim("roles", List.of("AGENT"))
            .build();

    var result = converter.convert(jwt);

    var expected = someAgentUser();
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_AGENT"));
  }

  @Test
  void shouldHandleMissingRolesClaim() {
    var jwt =
        jwtBuilder().subject(SOME_USER_UUID.toString()).claim("email", SOME_ADMIN_EMAIL).build();

    var result = converter.convert(jwt);

    assertThat(result.getAuthorities()).isEmpty();
    assertThat(((AuthenticatedUserToken) result).getPrincipal().roles()).isEmpty();
  }

  @Test
  void shouldExposeJwtAsCredentials() {
    var jwt =
        jwtBuilder()
            .subject(SOME_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    var result = converter.convert(jwt);

    assertThat(result.getCredentials()).isSameAs(jwt);
  }

  @Test
  void shouldUseEmailAsAuthenticationName() {
    var jwt =
        jwtBuilder()
            .subject(SOME_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    var result = converter.convert(jwt);

    assertThat(result.getName()).isEqualTo(SOME_CUSTOMER_EMAIL);
  }

  private static Jwt.Builder jwtBuilder() {
    return Jwt.withTokenValue("token-value")
        .header("alg", "RS256")
        .header("kid", "test-key-1")
        .issuedAt(ISSUED_AT)
        .expiresAt(EXPIRES_AT)
        .issuer("https://auth.stablepay.local")
        .audience(List.of("stablepay-api"));
  }
}
