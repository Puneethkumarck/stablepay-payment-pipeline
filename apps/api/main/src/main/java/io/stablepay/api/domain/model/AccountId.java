package io.stablepay.api.domain.model;

import java.util.UUID;

public record AccountId(UUID value) {

  public static AccountId of(UUID value) {
    return new AccountId(value);
  }
}
