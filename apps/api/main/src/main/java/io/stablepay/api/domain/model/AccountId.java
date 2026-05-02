package io.stablepay.api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

  public AccountId {
    Objects.requireNonNull(value, "value");
  }

  public static AccountId of(UUID value) {
    return new AccountId(value);
  }
}
