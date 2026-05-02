package io.stablepay.api.domain.statemachine;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class StateMachine<S, T extends StateProvider<S>> {

  private final Map<S, Map<S, TransitionAction<T, S>>> transitions;
  private final Function<String, ? extends RuntimeException> exceptionProvider;

  private StateMachine(
      Map<S, Map<S, TransitionAction<T, S>>> transitions,
      Function<String, ? extends RuntimeException> exceptionProvider) {
    this.transitions = transitions;
    this.exceptionProvider = exceptionProvider;
  }

  public static <S, T extends StateProvider<S>> Builder<S, T> builder() {
    return new Builder<>();
  }

  public boolean canTransition(S from, S to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    return transitions.getOrDefault(from, Map.of()).containsKey(to);
  }

  public Set<S> getValidPredecessors(S to) {
    Objects.requireNonNull(to, "to");
    return transitions.entrySet().stream()
        .filter(entry -> entry.getValue().containsKey(to))
        .map(Map.Entry::getKey)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  public StateChangedEvent<S> transition(T entity, S target) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(target, "target");
    var current = Objects.requireNonNull(entity.getCurrentState(), "currentState");
    var action = transitions.getOrDefault(current, Map.of()).get(target);
    if (action == null) {
      throw exceptionProvider.apply("Invalid transition from " + current + " to " + target);
    }
    return action.apply(entity);
  }

  public static final class Builder<S, T extends StateProvider<S>> {

    private final Map<S, Map<S, TransitionAction<T, S>>> transitions = new LinkedHashMap<>();
    private Function<String, ? extends RuntimeException> exceptionProvider =
        StateMachineException::new;

    private Builder() {}

    public Builder<S, T> withTransition(S from, S to, TransitionAction<T, S> action) {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
      Objects.requireNonNull(action, "action");
      transitions.computeIfAbsent(from, k -> new LinkedHashMap<>()).put(to, action);
      return this;
    }

    public Builder<S, T> withTransitionsFrom(S from, Set<S> tos, TransitionAction<T, S> action) {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(tos, "tos");
      Objects.requireNonNull(action, "action");
      tos.forEach(to -> withTransition(from, to, action));
      return this;
    }

    public Builder<S, T> withExceptionProvider(
        Function<String, ? extends RuntimeException> provider) {
      this.exceptionProvider = Objects.requireNonNull(provider, "exceptionProvider");
      return this;
    }

    public StateMachine<S, T> build() {
      var deepCopy = new HashMap<S, Map<S, TransitionAction<T, S>>>();
      transitions.forEach(
          (from, inner) ->
              deepCopy.put(from, Collections.unmodifiableMap(new LinkedHashMap<>(inner))));
      return new StateMachine<>(Collections.unmodifiableMap(deepCopy), exceptionProvider);
    }
  }
}
