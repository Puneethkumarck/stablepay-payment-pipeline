package io.stablepay.api.domain.model;

import java.util.UUID;

public record DlqId(UUID value) {

  public static DlqId of(UUID value) {
    return new DlqId(value);
  }
}
