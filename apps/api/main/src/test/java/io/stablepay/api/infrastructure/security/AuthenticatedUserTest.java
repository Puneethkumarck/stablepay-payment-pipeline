package io.stablepay.api.infrastructure.security;

import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.SOME_USER_ID;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someAgentUser;
import static io.stablepay.api.infrastructure.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

  @Test
  void shouldBuildCustomerUser() {
    var result =
        AuthenticatedUser.builder()
            .userId(SOME_USER_ID)
            .customerId(Optional.of(SOME_CUSTOMER_ID))
            .roles(Set.of(Role.CUSTOMER))
            .email(SOME_CUSTOMER_EMAIL)
            .build();

    var expected = someCustomerUser();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldBuildAdminUserWithoutCustomerId() {
    var result =
        AuthenticatedUser.builder()
            .userId(SOME_USER_ID)
            .customerId(Optional.empty())
            .roles(Set.of(Role.ADMIN))
            .email(SOME_ADMIN_EMAIL)
            .build();

    var expected = someAdminUser();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReportCustomerRole() {
    var result = someCustomerUser();

    assertThat(result.isCustomer()).isTrue();
    assertThat(result.isAdmin()).isFalse();
    assertThat(result.isAgent()).isFalse();
  }

  @Test
  void shouldReportAdminRole() {
    var result = someAdminUser();

    assertThat(result.isAdmin()).isTrue();
    assertThat(result.isCustomer()).isFalse();
    assertThat(result.isAgent()).isFalse();
  }

  @Test
  void shouldReportAgentRole() {
    var result = someAgentUser();

    assertThat(result.isAgent()).isTrue();
    assertThat(result.isCustomer()).isFalse();
    assertThat(result.isAdmin()).isFalse();
  }

  @Test
  void shouldReturnCustomerIdWhenPresent() {
    var user = someCustomerUser();

    var result = user.requireCustomerId();

    assertThat(result).usingRecursiveComparison().isEqualTo(SOME_CUSTOMER_ID);
  }

  @Test
  void shouldThrowWhenAdminAttemptsToRequireCustomerId() {
    var user = someAdminUser();

    assertThatThrownBy(user::requireCustomerId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Endpoint requires CUSTOMER role");
  }

  @Test
  void shouldThrowWhenAgentAttemptsToRequireCustomerId() {
    var user = someAgentUser();

    assertThatThrownBy(user::requireCustomerId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Endpoint requires CUSTOMER role");
  }

  @Test
  void shouldRejectNullUserId() {
    assertThatThrownBy(
            () ->
                AuthenticatedUser.builder()
                    .userId(null)
                    .customerId(Optional.empty())
                    .roles(Set.of(Role.ADMIN))
                    .email(SOME_ADMIN_EMAIL)
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("userId");
  }

  @Test
  void shouldRejectNullCustomerIdOptional() {
    assertThatThrownBy(
            () ->
                AuthenticatedUser.builder()
                    .userId(SOME_USER_ID)
                    .customerId(null)
                    .roles(Set.of(Role.ADMIN))
                    .email(SOME_ADMIN_EMAIL)
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("customerId");
  }
}
