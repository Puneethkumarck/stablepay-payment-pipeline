package io.stablepay.api.infrastructure.idempotency;

import static io.stablepay.api.infrastructure.idempotency.fixtures.IdempotencyFixtures.SOME_NOW;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.stablepay.api.domain.port.IdempotencyRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupJobTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(SOME_NOW, ZoneOffset.UTC);

  @Mock private IdempotencyRepository idempotencyRepository;

  @Test
  void shouldDeleteExpiredKeysUsingCurrentTime() {
    // given
    var job = new IdempotencyCleanupJob(idempotencyRepository, FIXED_CLOCK);
    given(idempotencyRepository.deleteExpired(SOME_NOW)).willReturn(5);

    // when
    job.cleanup();

    // then
    then(idempotencyRepository).should().deleteExpired(SOME_NOW);
  }
}
