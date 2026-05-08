package io.stablepay.api.infrastructure.security;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.JwtFixtures.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.stablepay.api.application.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class JwtAuthenticationIT {

  private final JwtToAuthenticatedUserConverter converter = new JwtToAuthenticatedUserConverter();

  @Test
  void shouldSetAuthenticatedTrueForCustomerJwt() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when
    var token = converter.convert(jwt);

    // then
    assertThat(token.isAuthenticated()).isTrue();
  }

  @Test
  void shouldExposeRequireCustomerIdForCustomerJwt() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when
    var token = converter.convert(jwt);
    var principal = (AuthenticatedUser) token.getPrincipal();

    // then
    assertThat(principal.requireCustomerId().value()).isEqualTo(SOME_CUSTOMER_UUID);
  }

  @Test
  void shouldThrowWhenAdminCallsRequireCustomerId() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_ADMIN_USER_UUID.toString())
            .claim("email", SOME_ADMIN_EMAIL)
            .claim("roles", List.of("ADMIN"))
            .build();

    // when
    var token = converter.convert(jwt);
    var principal = (AuthenticatedUser) token.getPrincipal();

    // then
    assertThatThrownBy(principal::requireCustomerId).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldMapMultipleRolesToAuthorities() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_ADMIN_USER_UUID.toString())
            .claim("email", SOME_ADMIN_EMAIL)
            .claim("roles", List.of("ADMIN", "AGENT"))
            .build();

    // when
    var token = converter.convert(jwt);

    // then
    assertThat(token.getAuthorities())
        .extracting(Object::toString)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_AGENT");
  }
}
