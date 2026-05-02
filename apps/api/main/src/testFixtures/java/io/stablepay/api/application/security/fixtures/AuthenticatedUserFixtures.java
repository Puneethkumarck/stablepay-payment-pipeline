package io.stablepay.api.application.security.fixtures;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.UserId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AuthenticatedUserFixtures {

  public static final UUID SOME_CUSTOMER_USER_UUID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  public static final UUID SOME_ADMIN_USER_UUID =
      UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  public static final UUID SOME_AGENT_USER_UUID =
      UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  public static final UUID SOME_CUSTOMER_UUID =
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  public static final UserId SOME_CUSTOMER_USER_ID = UserId.of(SOME_CUSTOMER_USER_UUID);
  public static final UserId SOME_ADMIN_USER_ID = UserId.of(SOME_ADMIN_USER_UUID);
  public static final UserId SOME_AGENT_USER_ID = UserId.of(SOME_AGENT_USER_UUID);
  public static final CustomerId SOME_CUSTOMER_ID = CustomerId.of(SOME_CUSTOMER_UUID);

  public static final String SOME_CUSTOMER_EMAIL = "alice@stablepay.io";
  public static final String SOME_ADMIN_EMAIL = "admin@stablepay.io";
  public static final String SOME_AGENT_EMAIL = "agent@stablepay.io";

  private AuthenticatedUserFixtures() {}

  public static AuthenticatedUser someCustomerUser() {
    return AuthenticatedUser.builder()
        .userId(SOME_CUSTOMER_USER_ID)
        .customerId(Optional.of(SOME_CUSTOMER_ID))
        .roles(Set.of(Role.CUSTOMER))
        .email(SOME_CUSTOMER_EMAIL)
        .build();
  }

  public static AuthenticatedUser someAdminUser() {
    return AuthenticatedUser.builder()
        .userId(SOME_ADMIN_USER_ID)
        .customerId(Optional.empty())
        .roles(Set.of(Role.ADMIN))
        .email(SOME_ADMIN_EMAIL)
        .build();
  }

  public static AuthenticatedUser someAgentUser() {
    return AuthenticatedUser.builder()
        .userId(SOME_AGENT_USER_ID)
        .customerId(Optional.empty())
        .roles(Set.of(Role.AGENT))
        .email(SOME_AGENT_EMAIL)
        .build();
  }
}
