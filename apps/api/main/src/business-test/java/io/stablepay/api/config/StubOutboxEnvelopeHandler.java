package io.stablepay.api.config;

import io.stablepay.api.infrastructure.outbox.OutboxEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StubOutboxEnvelopeHandler {

  @io.namastack.outbox.annotation.OutboxHandler
  public void handle(OutboxEnvelope envelope) {
    log.info("Outbox record processed: topic={}", envelope.topic());
  }
}
