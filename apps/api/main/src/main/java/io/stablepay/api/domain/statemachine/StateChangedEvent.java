package io.stablepay.api.domain.statemachine;

import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record StateChangedEvent<S>(S from, S to) {

  public StateChangedEvent {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
  }
}
