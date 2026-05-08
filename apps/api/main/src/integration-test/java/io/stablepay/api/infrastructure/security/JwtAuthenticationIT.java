package io.stablepay.api.infrastructure.security;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.JwtFixtures.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.UserId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class JwtAuthenticationIT {

  private final JwtToAuthenticatedUserConverter converter = new JwtToAuthenticatedUserConverter();

  @Nested
  class CustomerJwt {

    @Test
    void shouldDecodeCustomerJwtWithCustomerIdClaim() {
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
      var expected =
          AuthenticatedUser.builder()
              .userId(UserId.of(SOME_CUSTOMER_USER_UUID))
              .customerId(Optional.of(CustomerId.of(SOME_CUSTOMER_UUID)))
              .roles(Set.of(Role.CUSTOMER))
              .email(SOME_CUSTOMER_EMAIL)
              .build();
      assertThat(token.getPrincipal()).usingRecursiveComparison().isEqualTo(expected);
    }

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
  }

  @Nested
  class AdminJwt {

    @Test
    void shouldDecodeAdminJwtWithoutCustomerIdClaim() {
      // given
      var jwt =
          jwtBuilder()
              .subject(SOME_ADMIN_USER_UUID.toString())
              .claim("email", SOME_ADMIN_EMAIL)
              .claim("roles", List.of("ADMIN"))
              .build();

      // when
      var token = converter.convert(jwt);

      // then
      var expected =
          AuthenticatedUser.builder()
              .userId(UserId.of(SOME_ADMIN_USER_UUID))
              .customerId(Optional.empty())
              .roles(Set.of(Role.ADMIN))
              .email(SOME_ADMIN_EMAIL)
              .build();
      assertThat(token.getPrincipal()).usingRecursiveComparison().isEqualTo(expected);
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
  }

  @Nested
  class AgentJwt {

    @Test
    void shouldDecodeAgentJwtWithoutCustomerIdClaim() {
      // given
      var jwt =
          jwtBuilder()
              .subject(SOME_AGENT_USER_UUID.toString())
              .claim("email", SOME_AGENT_EMAIL)
              .claim("roles", List.of("AGENT"))
              .build();

      // when
      var token = converter.convert(jwt);

      // then
      var expected =
          AuthenticatedUser.builder()
              .userId(UserId.of(SOME_AGENT_USER_UUID))
              .customerId(Optional.empty())
              .roles(Set.of(Role.AGENT))
              .email(SOME_AGENT_EMAIL)
              .build();
      assertThat(token.getPrincipal()).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class RoleMappings {

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

    @Test
    void shouldHandleMissingRolesClaim() {
      // given
      var jwt =
          jwtBuilder()
              .subject(SOME_ADMIN_USER_UUID.toString())
              .claim("email", SOME_ADMIN_EMAIL)
              .build();

      // when
      var token = converter.convert(jwt);

      // then
      var principal = (AuthenticatedUser) token.getPrincipal();
      assertThat(principal.roles()).isEmpty();
    }
  }
}
