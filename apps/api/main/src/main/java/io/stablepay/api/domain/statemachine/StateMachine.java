package io.stablepay.api.domain.statemachine;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class StateMachine<S extends Enum<S>> {

  private final Map<S, Set<S>> transitions;

  public StateMachine(Map<S, Set<S>> transitions) {
    Objects.requireNonNull(transitions, "transitions");
    this.transitions =
        transitions.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));
  }

  public boolean canTransition(S from, S to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    return transitions.getOrDefault(from, Set.of()).contains(to);
  }

  public TransitionResult<S> validate(S from, S to) {
    return canTransition(from, to)
        ? new TransitionResult.Valid<>(to)
        : new TransitionResult.Invalid<>(from, to);
  }

  public sealed interface TransitionResult<S extends Enum<S>>
      permits TransitionResult.Valid, TransitionResult.Invalid {

    record Valid<S extends Enum<S>>(S to) implements TransitionResult<S> {
      public Valid {
        Objects.requireNonNull(to, "to");
      }
    }

    record Invalid<S extends Enum<S>>(S from, S to) implements TransitionResult<S> {
      public Invalid {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
      }
    }
  }
}
