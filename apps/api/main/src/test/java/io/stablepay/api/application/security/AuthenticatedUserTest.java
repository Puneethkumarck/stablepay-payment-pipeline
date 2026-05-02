package io.stablepay.api.application.security;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_ADMIN_USER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_EMAIL;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_USER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAgentUser;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

  @Test
  void shouldBuildCustomerUser() {
    // when
    var result =
        AuthenticatedUser.builder()
            .userId(SOME_CUSTOMER_USER_ID)
            .customerId(Optional.of(SOME_CUSTOMER_ID))
            .roles(Set.of(Role.CUSTOMER))
            .email(SOME_CUSTOMER_EMAIL)
            .build();

    // then
    var expected = someCustomerUser();
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldBuildAdminUserWithoutCustomerId() {
    // when
    var result =
        AuthenticatedUser.builder()
            .userId(SOME_ADMIN_USER_ID)
            .customerId(Optional.empty())
            .roles(Set.of(Role.ADMIN))
            .email(SOME_ADMIN_EMAIL)
            .build();

    // then
    var expected = someAdminUser();
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReportCustomerRole() {
    // given
    var result = someCustomerUser();

    // then
    assertThat(result.isCustomer()).isTrue();
    assertThat(result.isAdmin()).isFalse();
    assertThat(result.isAgent()).isFalse();
  }

  @Test
  void shouldReportAdminRole() {
    // given
    var result = someAdminUser();

    // then
    assertThat(result.isAdmin()).isTrue();
    assertThat(result.isCustomer()).isFalse();
    assertThat(result.isAgent()).isFalse();
  }

  @Test
  void shouldReportAgentRole() {
    // given
    var result = someAgentUser();

    // then
    assertThat(result.isAgent()).isTrue();
    assertThat(result.isCustomer()).isFalse();
    assertThat(result.isAdmin()).isFalse();
  }

  @Test
  void shouldReturnCustomerIdWhenPresent() {
    // given
    var user = someCustomerUser();

    // when
    var result = user.requireCustomerId();

    // then
    assertThat(result).usingRecursiveComparison().isEqualTo(SOME_CUSTOMER_ID);
  }

  @Test
  void shouldThrowWhenAdminAttemptsToRequireCustomerId() {
    // given
    var user = someAdminUser();

    // when/then
    assertThatThrownBy(user::requireCustomerId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Endpoint requires CUSTOMER role");
  }

  @Test
  void shouldThrowWhenAgentAttemptsToRequireCustomerId() {
    // given
    var user = someAgentUser();

    // when/then
    assertThatThrownBy(user::requireCustomerId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Endpoint requires CUSTOMER role");
  }

  @Test
  void shouldRejectNullUserId() {
    // when/then
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
    // when/then
    assertThatThrownBy(
            () ->
                AuthenticatedUser.builder()
                    .userId(SOME_ADMIN_USER_ID)
                    .customerId(null)
                    .roles(Set.of(Role.ADMIN))
                    .email(SOME_ADMIN_EMAIL)
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("customerId");
  }
}
