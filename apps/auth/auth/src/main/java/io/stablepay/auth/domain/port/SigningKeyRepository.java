package io.stablepay.auth.domain.port;

import io.stablepay.auth.domain.model.SigningKey;
import java.util.List;
import java.util.Optional;

public interface SigningKeyRepository {

  Optional<SigningKey> findActive();

  List<SigningKey> findAll();

  void save(SigningKey key);
}
