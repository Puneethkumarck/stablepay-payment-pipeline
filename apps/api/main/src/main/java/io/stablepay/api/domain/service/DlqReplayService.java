package io.stablepay.api.domain.service;

import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.model.UserId;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.port.OutboxRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReplayService {

  public static final String REPLAY_TOPIC = "dlq.replay.command.v1";

  private final DlqRepository dlqRepository;
  private final OutboxRepository outboxRepository;

  @Transactional
  public DlqEvent replay(DlqId id, UserId userId) {
    var event =
        dlqRepository
            .findByIdAdmin(id)
            .orElseThrow(() -> new NotFoundException("DLQ event", id.value().toString()));
    var payload = buildPayload(id, userId);
    outboxRepository.publishIdempotent("dlq-replay:" + id.value(), REPLAY_TOPIC, payload);
    log.info("DLQ replay triggered for dlq_id={}", id.value());
    return event;
  }

  private static byte[] buildPayload(DlqId dlqId, UserId userId) {
    return ("{\"dlq_id\":\"" + dlqId.value() + "\",\"triggered_by\":\"" + userId.value() + "\"}")
        .getBytes(StandardCharsets.UTF_8);
  }
}
