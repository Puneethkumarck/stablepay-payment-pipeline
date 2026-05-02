package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CachedResponse;
import io.stablepay.api.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRepository {

  Optional<CachedResponse> findActive(String key, UserId userId, Instant now);

  boolean tryAcquire(String key, UserId userId, Instant expiresAt);

  void save(String key, UserId userId, CachedResponse response);

  int deleteExpired(Instant now);
}
