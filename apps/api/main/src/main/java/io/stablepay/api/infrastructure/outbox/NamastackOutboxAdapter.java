package io.stablepay.api.infrastructure.outbox;

import io.namastack.outbox.Outbox;
import io.stablepay.api.domain.port.OutboxRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Idempotency at the publish layer is enforced upstream by IdempotencyRepository
// (see V1__SPP-88_idempotency_keys.sql + PostgresIdempotencyRepository): the request handler
// short-circuits on a cached response before this adapter is invoked. Namastack itself does
// not enforce uniqueness on record_key, by design (retries reuse the key).
@Component
@RequiredArgsConstructor
@Slf4j
public class NamastackOutboxAdapter implements OutboxRepository {

  private final Outbox outbox;

  @Override
  public void publishIdempotent(String idempotencyKey, String topic, byte[] payload) {
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payload, "payload");
    var envelope = OutboxEnvelope.of(topic, payload);
    outbox.schedule(envelope, idempotencyKey);
  }
}
