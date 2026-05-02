package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix. NOTE: this repository is infra-scoped (keyed by UserId, not CustomerId) and is explicitly
 * excluded from the customer-scope ArchUnit rule.
 */
public interface IdempotencyRepository {

  Optional<CachedResponse> findActive(String key, UserId userId, Instant now);

  void save(String key, UserId userId, int status, byte[] body, Instant expiresAt);

  int deleteExpired(Instant now);
}
