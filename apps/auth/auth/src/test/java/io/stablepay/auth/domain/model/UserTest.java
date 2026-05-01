package io.stablepay.auth.domain.model;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_EMAIL;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_LATER_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_PASSWORD_HASH;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void shouldBuildCustomerUserWithCustomerId() {
    // when
    var actual =
        User.builder()
            .id(SOME_USER_ID)
            .customerId(Optional.of(SOME_CUSTOMER_ID))
            .email(SOME_EMAIL)
            .passwordHash(SOME_PASSWORD_HASH)
            .roles(Set.of(Role.CUSTOMER))
            .createdAt(SOME_INSTANT)
            .updatedAt(SOME_INSTANT)
            .build();

    // then
    var expected =
        new User(
            SOME_USER_ID,
            Optional.of(SOME_CUSTOMER_ID),
            SOME_EMAIL,
            SOME_PASSWORD_HASH,
            Set.of(Role.CUSTOMER),
            SOME_INSTANT,
            SOME_INSTANT);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldBuildAdminUserWithEmptyCustomerId() {
    // when
    var actual =
        User.builder()
            .id(SOME_USER_ID)
            .customerId(Optional.empty())
            .email("admin@stablepay.io")
            .passwordHash(SOME_PASSWORD_HASH)
            .roles(Set.of(Role.ADMIN))
            .createdAt(SOME_INSTANT)
            .updatedAt(SOME_INSTANT)
            .build();

    // then
    var expected =
        new User(
            SOME_USER_ID,
            Optional.empty(),
            "admin@stablepay.io",
            SOME_PASSWORD_HASH,
            Set.of(Role.ADMIN),
            SOME_INSTANT,
            SOME_INSTANT);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturnNewInstanceWithUpdatedFieldViaToBuilder() {
    // given
    var original =
        User.builder()
            .id(SOME_USER_ID)
            .customerId(Optional.of(SOME_CUSTOMER_ID))
            .email(SOME_EMAIL)
            .passwordHash(SOME_PASSWORD_HASH)
            .roles(Set.of(Role.CUSTOMER))
            .createdAt(SOME_INSTANT)
            .updatedAt(SOME_INSTANT)
            .build();

    // when
    var actual = original.toBuilder().updatedAt(SOME_LATER_INSTANT).build();

    // then
    var expected =
        new User(
            SOME_USER_ID,
            Optional.of(SOME_CUSTOMER_ID),
            SOME_EMAIL,
            SOME_PASSWORD_HASH,
            Set.of(Role.CUSTOMER),
            SOME_INSTANT,
            SOME_LATER_INSTANT);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
