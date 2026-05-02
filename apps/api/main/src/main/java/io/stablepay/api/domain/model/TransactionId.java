package io.stablepay.api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {

  public TransactionId {
    Objects.requireNonNull(value, "value");
  }

  public static TransactionId of(UUID value) {
    return new TransactionId(value);
  }
}
