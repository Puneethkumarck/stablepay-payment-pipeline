package io.stablepay.api.domain.statemachine;

public interface StateProvider<S> {

  S getCurrentState();
}
