package io.stablepay.api.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OutboxEnvelopeHandler {

  @io.namastack.outbox.annotation.OutboxHandler
  public void handle(OutboxEnvelope envelope) {
    log.info("Outbox record processed: topic={}", envelope.topic());
  }
}
