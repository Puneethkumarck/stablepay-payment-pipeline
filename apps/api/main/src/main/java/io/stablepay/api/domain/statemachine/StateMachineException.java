package io.stablepay.api.domain.statemachine;

public class StateMachineException extends RuntimeException {

  public StateMachineException(String message) {
    super(message);
  }
}
