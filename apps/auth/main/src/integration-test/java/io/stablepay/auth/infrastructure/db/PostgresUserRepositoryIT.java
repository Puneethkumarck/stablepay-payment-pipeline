package io.stablepay.auth.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.auth.domain.model.CustomerId;
import io.stablepay.auth.domain.model.Role;
import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresUserRepositoryIT extends PostgresRepositoryIntegrationTest {

  private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID AGENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Test
  void findByEmailReturnsAliceWithCustomerIdAndAdminCustomerRoles() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findByEmail("alice@stablepay.io");

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("value.createdAt", "value.updatedAt")
        .isEqualTo(
            Optional.of(
                User.builder()
                    .id(UserId.of(ALICE_ID))
                    .customerId(Optional.of(CustomerId.of(ALICE_ID)))
                    .email("alice@stablepay.io")
                    .passwordHash(passwordOf(ALICE_ID))
                    .roles(Set.of(Role.ADMIN, Role.CUSTOMER))
                    .createdAt(Instant.EPOCH)
                    .updatedAt(Instant.EPOCH)
                    .build()));
  }

  @Test
  void findByEmailIsCaseInsensitive() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findByEmail("ALICE@STABLEPAY.IO");

    assertThat(actual).map(User::id).contains(UserId.of(ALICE_ID));
  }

  @Test
  void findByEmailReturnsEmptyWhenUserMissing() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findByEmail("ghost@stablepay.io");

    assertThat(actual).isEmpty();
  }

  @Test
  void findByIdReturnsBobWithCustomerScopeAndCustomerRole() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findById(UserId.of(BOB_ID));

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("value.createdAt", "value.updatedAt")
        .isEqualTo(
            Optional.of(
                User.builder()
                    .id(UserId.of(BOB_ID))
                    .customerId(Optional.of(CustomerId.of(BOB_ID)))
                    .email("bob@stablepay.io")
                    .passwordHash(passwordOf(BOB_ID))
                    .roles(Set.of(Role.CUSTOMER))
                    .createdAt(Instant.EPOCH)
                    .updatedAt(Instant.EPOCH)
                    .build()));
  }

  @Test
  void findByIdMapsNullCustomerIdToEmptyOptionalForAdminUser() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findById(UserId.of(ADMIN_ID));

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("value.createdAt", "value.updatedAt")
        .isEqualTo(
            Optional.of(
                User.builder()
                    .id(UserId.of(ADMIN_ID))
                    .customerId(Optional.empty())
                    .email("admin@stablepay.io")
                    .passwordHash(passwordOf(ADMIN_ID))
                    .roles(Set.of(Role.ADMIN))
                    .createdAt(Instant.EPOCH)
                    .updatedAt(Instant.EPOCH)
                    .build()));
  }

  @Test
  void findByIdMapsNullCustomerIdToEmptyOptionalForAgentUser() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findById(UserId.of(AGENT_ID));

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringFields("value.createdAt", "value.updatedAt")
        .isEqualTo(
            Optional.of(
                User.builder()
                    .id(UserId.of(AGENT_ID))
                    .customerId(Optional.empty())
                    .email("agent@stablepay.io")
                    .passwordHash(passwordOf(AGENT_ID))
                    .roles(Set.of(Role.AGENT))
                    .createdAt(Instant.EPOCH)
                    .updatedAt(Instant.EPOCH)
                    .build()));
  }

  @Test
  void findByIdReturnsEmptyWhenUserMissing() {
    var repository = new PostgresUserRepository(namedJdbc);

    var actual = repository.findById(UserId.of(UUID.randomUUID()));

    assertThat(actual).isEmpty();
  }

  private static String passwordOf(UUID userId) {
    return jdbc.queryForObject(
        "SELECT password FROM users WHERE user_id = ?", String.class, userId);
  }
}
