package io.stablepay.api.domain.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class StateMachineTest {

  private enum TestStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
  }

  private record TestEntity(TestStatus status) implements StateProvider<TestStatus> {
    @Override
    public TestStatus getCurrentState() {
      return status;
    }
  }

  private static StateMachine<TestStatus, TestEntity> defaultMachine() {
    return StateMachine.<TestStatus, TestEntity>builder()
        .withTransition(TestStatus.PENDING, TestStatus.ACTIVE, TransitionAction.noAction())
        .withTransitionsFrom(
            TestStatus.ACTIVE,
            Set.of(TestStatus.COMPLETED, TestStatus.CANCELLED),
            TransitionAction.noAction())
        .build();
  }

  @Test
  void canTransition_validEdge_returnsTrue() {
    // given
    var machine = defaultMachine();

    // when
    var actual = machine.canTransition(TestStatus.PENDING, TestStatus.ACTIVE);

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void canTransition_invalidEdge_returnsFalse() {
    // given
    var machine = defaultMachine();

    // when
    var actual = machine.canTransition(TestStatus.PENDING, TestStatus.COMPLETED);

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void getValidPredecessors_returnsAllFromStatesThatTransitionToTarget() {
    // given
    var machine =
        StateMachine.<TestStatus, TestEntity>builder()
            .withTransition(TestStatus.PENDING, TestStatus.ACTIVE, TransitionAction.noAction())
            .withTransition(TestStatus.CANCELLED, TestStatus.ACTIVE, TransitionAction.noAction())
            .build();
    var expected = Set.of(TestStatus.PENDING, TestStatus.CANCELLED);

    // when
    var actual = machine.getValidPredecessors(TestStatus.ACTIVE);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void transition_validEdge_returnsEventFromAction() {
    // given
    var emittedEvent = new StateChangedEvent<>(TestStatus.PENDING, TestStatus.ACTIVE);
    var machine =
        StateMachine.<TestStatus, TestEntity>builder()
            .withTransition(TestStatus.PENDING, TestStatus.ACTIVE, entity -> emittedEvent)
            .build();
    var entity = new TestEntity(TestStatus.PENDING);

    // when
    var actual = machine.transition(entity, TestStatus.ACTIVE);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(emittedEvent);
  }

  @Test
  void transition_noActionEdge_returnsNullEvent() {
    // given
    var machine = defaultMachine();
    var entity = new TestEntity(TestStatus.PENDING);

    // when
    var actual = machine.transition(entity, TestStatus.ACTIVE);

    // then
    assertThat(actual).isNull();
  }

  @Test
  void transition_invalidEdge_throwsDefaultStateMachineException() {
    // given
    var machine = defaultMachine();
    var entity = new TestEntity(TestStatus.PENDING);

    // when / then
    assertThatThrownBy(() -> machine.transition(entity, TestStatus.COMPLETED))
        .isInstanceOf(StateMachineException.class)
        .hasMessage("Invalid transition from PENDING to COMPLETED");
  }

  @Test
  void transition_invalidEdge_usesCustomExceptionProvider() {
    // given
    var machine =
        StateMachine.<TestStatus, TestEntity>builder()
            .withTransition(TestStatus.PENDING, TestStatus.ACTIVE, TransitionAction.noAction())
            .withExceptionProvider(IllegalStateException::new)
            .build();
    var entity = new TestEntity(TestStatus.PENDING);

    // when / then
    assertThatThrownBy(() -> machine.transition(entity, TestStatus.COMPLETED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid transition from PENDING to COMPLETED");
  }

  @Test
  void builder_nullTransitionFrom_throwsNpe() {
    // given
    var builder = StateMachine.<TestStatus, TestEntity>builder();

    // when / then
    assertThatThrownBy(
            () -> builder.withTransition(null, TestStatus.ACTIVE, TransitionAction.noAction()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void builder_defensiveCopy_doesNotShareInternalMaps() {
    // given
    var machine = defaultMachine();
    var snapshot = machine.getValidPredecessors(TestStatus.ACTIVE);
    var expected = Set.of(TestStatus.PENDING);

    // when / then
    assertThat(snapshot).usingRecursiveComparison().isEqualTo(expected);
  }
}
