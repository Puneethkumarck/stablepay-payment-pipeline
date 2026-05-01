package io.stablepay.auth.domain.model;

import java.util.UUID;

public record CustomerId(UUID value) {

  public static CustomerId of(UUID value) {
    return new CustomerId(value);
  }
}
