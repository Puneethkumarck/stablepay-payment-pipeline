package io.stablepay.api.infrastructure.security;

import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_AGENT_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAgentUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.infrastructure.security.fixtures.JwtFixtures.jwtBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtToAuthenticatedUserConverterTest {

  private final JwtToAuthenticatedUserConverter converter = new JwtToAuthenticatedUserConverter();

  @Test
  void shouldConvertCustomerJwtWithCustomerIdClaim() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when
    var result = converter.convert(jwt);

    // then
    var expected = someCustomerUser();
    assertThat(result).isInstanceOf(AuthenticatedUserToken.class);
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  @Test
  void shouldConvertAdminJwtWithoutCustomerIdClaimWithoutNpe() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_ADMIN_USER_UUID.toString())
            .claim("email", SOME_ADMIN_EMAIL)
            .claim("roles", List.of("ADMIN"))
            .build();

    // when
    var result = converter.convert(jwt);

    // then
    var expected = someAdminUser();
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  void shouldConvertAgentJwtWithoutCustomerIdClaimWithoutNpe() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_AGENT_USER_UUID.toString())
            .claim("email", SOME_AGENT_EMAIL)
            .claim("roles", List.of("AGENT"))
            .build();

    // when
    var result = converter.convert(jwt);

    // then
    var expected = someAgentUser();
    assertThat(((AuthenticatedUserToken) result).getPrincipal())
        .usingRecursiveComparison()
        .isEqualTo(expected);
    assertThat(result.getAuthorities()).containsOnly(new SimpleGrantedAuthority("ROLE_AGENT"));
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
    var result = converter.convert(jwt);

    // then
    assertThat(result.getAuthorities()).isEmpty();
    assertThat(((AuthenticatedUserToken) result).getPrincipal().roles()).isEmpty();
  }

  @Test
  void shouldExposeJwtAsCredentials() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when
    var result = converter.convert(jwt);

    // then
    assertThat(result.getCredentials()).isSameAs(jwt);
  }

  @Test
  void shouldUseEmailAsAuthenticationName() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when
    var result = converter.convert(jwt);

    // then
    assertThat(result.getName()).isEqualTo(SOME_CUSTOMER_EMAIL);
  }

  @Test
  void shouldRejectJwtWithInvalidSubjectUuid() {
    // given
    var jwt =
        jwtBuilder()
            .subject("not-a-uuid")
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", SOME_CUSTOMER_UUID.toString())
            .build();

    // when/then
    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UUID");
  }

  @Test
  void shouldRejectJwtWithInvalidCustomerIdUuid() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .claim("customer_id", "not-a-uuid")
            .build();

    // when/then
    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UUID");
  }

  @Test
  void shouldRejectJwtWithUnknownRole() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_CUSTOMER_USER_UUID.toString())
            .claim("email", SOME_CUSTOMER_EMAIL)
            .claim("roles", List.of("WIZARD"))
            .build();

    // when/then
    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("WIZARD");
  }

  @Test
  void shouldRejectJwtWithMissingEmailClaim() {
    // given
    var jwt =
        jwtBuilder()
            .subject(SOME_ADMIN_USER_UUID.toString())
            .claim("roles", List.of("ADMIN"))
            .build();

    // when/then
    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("email");
  }
}
