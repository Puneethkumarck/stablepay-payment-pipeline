package io.stablepay.api.domain.port;

public interface OutboxRepository {

  void publishIdempotent(String idempotencyKey, String topic, byte[] payload);
}
