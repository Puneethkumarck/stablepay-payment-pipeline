package io.stablepay.api.domain.model;

import java.util.UUID;

public record TransactionId(UUID value) {

  public static TransactionId of(UUID value) {
    return new TransactionId(value);
  }
}
