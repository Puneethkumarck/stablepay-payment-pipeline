package io.stablepay.api.domain.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.stablepay.api.domain.statemachine.StateMachine.TransitionResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StateMachineTest {

  private enum TestStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
  }

  @Test
  void canTransition_validEdge_returnsTrue() {
    // given
    var machine =
        new StateMachine<TestStatus>(
            Map.of(
                TestStatus.PENDING,
                Set.of(TestStatus.ACTIVE),
                TestStatus.ACTIVE,
                Set.of(TestStatus.COMPLETED, TestStatus.CANCELLED)));

    // when
    var actual = machine.canTransition(TestStatus.PENDING, TestStatus.ACTIVE);

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void canTransition_invalidEdge_returnsFalse() {
    // given
    var machine =
        new StateMachine<TestStatus>(
            Map.of(
                TestStatus.PENDING,
                Set.of(TestStatus.ACTIVE),
                TestStatus.ACTIVE,
                Set.of(TestStatus.COMPLETED, TestStatus.CANCELLED)));

    // when
    var actual = machine.canTransition(TestStatus.PENDING, TestStatus.COMPLETED);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void validate_validEdge_returnsValid_recursiveComparison() {
    // given
    var machine =
        new StateMachine<TestStatus>(Map.of(TestStatus.PENDING, Set.of(TestStatus.ACTIVE)));
    var expected = new TransitionResult.Valid<>(TestStatus.ACTIVE);

    // when
    var actual = machine.validate(TestStatus.PENDING, TestStatus.ACTIVE);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void validate_invalidEdge_returnsInvalid_recursiveComparison() {
    // given
    var machine =
        new StateMachine<TestStatus>(Map.of(TestStatus.PENDING, Set.of(TestStatus.ACTIVE)));
    var expected = new TransitionResult.Invalid<>(TestStatus.PENDING, TestStatus.COMPLETED);

    // when
    var actual = machine.validate(TestStatus.PENDING, TestStatus.COMPLETED);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void validate_unknownFromState_returnsInvalid() {
    // given
    var machine =
        new StateMachine<TestStatus>(Map.of(TestStatus.PENDING, Set.of(TestStatus.ACTIVE)));
    var expected = new TransitionResult.Invalid<>(TestStatus.CANCELLED, TestStatus.ACTIVE);

    // when
    var actual = machine.validate(TestStatus.CANCELLED, TestStatus.ACTIVE);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void constructor_nullTransitions_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> new StateMachine<TestStatus>(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_defensiveCopy_innerSetMutated_machineUnaffected() {
    // given
    var inner = new HashSet<TestStatus>();
    inner.add(TestStatus.ACTIVE);
    var transitions = new HashMap<TestStatus, Set<TestStatus>>();
    transitions.put(TestStatus.PENDING, inner);
    var machine = new StateMachine<TestStatus>(transitions);
    inner.clear();
    inner.add(TestStatus.COMPLETED);

    // when
    var actual = machine.canTransition(TestStatus.PENDING, TestStatus.ACTIVE);

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void transitionResult_isSealed_validIsTransitionResult() {
    // given
    var valid = new TransitionResult.Valid<>(TestStatus.ACTIVE);

    // when / then
    assertThat(valid).isInstanceOf(TransitionResult.class);
  }
}
