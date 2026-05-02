package io.stablepay.api.domain.model;

import java.util.Objects;
import java.util.UUID;

public record FlowId(UUID value) {

  public FlowId {
    Objects.requireNonNull(value, "value");
  }

  public static FlowId of(UUID value) {
    return new FlowId(value);
  }
}
