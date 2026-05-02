package io.stablepay.api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

  public CustomerId {
    Objects.requireNonNull(value, "value");
  }

  public static CustomerId of(UUID value) {
    return new CustomerId(value);
  }
}
