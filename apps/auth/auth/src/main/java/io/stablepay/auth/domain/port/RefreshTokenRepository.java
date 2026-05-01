package io.stablepay.auth.domain.port;

import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.RefreshTokenId;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

  void save(RefreshToken token);

  Optional<RefreshToken> findActiveByHash(String tokenHash, Instant now);

  void revoke(RefreshTokenId id, Instant at);

  int deleteRevokedAndExpiredOlderThan(Instant cutoff);
}
