package io.stablepay.api.domain.model;

import java.util.UUID;

public record FlowId(UUID value) {

  public static FlowId of(UUID value) {
    return new FlowId(value);
  }
}
