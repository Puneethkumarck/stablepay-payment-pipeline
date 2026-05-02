package io.stablepay.api.domain.port;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix. NOTE: this repository is infra-scoped (event publishing) and is explicitly excluded from
 * the customer-scope ArchUnit rule.
 */
public interface OutboxRepository {

  void publishIdempotent(String idempotencyKey, String topic, byte[] payload);
}
