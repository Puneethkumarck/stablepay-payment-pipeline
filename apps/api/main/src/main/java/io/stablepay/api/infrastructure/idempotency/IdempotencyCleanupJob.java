package io.stablepay.api.infrastructure.idempotency;

import io.stablepay.api.domain.port.IdempotencyRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupJob {

  private final IdempotencyRepository idempotencyRepository;
  private final Clock clock;

  @Scheduled(cron = "0 30 4 * * *")
  public void cleanup() {
    var deleted = idempotencyRepository.deleteExpired(clock.instant());
    log.info("Deleted {} expired idempotency keys", deleted);
  }
}
