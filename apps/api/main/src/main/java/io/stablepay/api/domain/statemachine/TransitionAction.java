package io.stablepay.api.domain.statemachine;

@FunctionalInterface
public interface TransitionAction<T, S> {

  StateChangedEvent<S> apply(T entity);

  static <T, S> TransitionAction<T, S> noAction() {
    return entity -> null;
  }
}
