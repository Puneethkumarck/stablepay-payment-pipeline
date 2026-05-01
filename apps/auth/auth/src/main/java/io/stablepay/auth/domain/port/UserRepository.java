package io.stablepay.auth.domain.port;

import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.model.UserId;
import java.util.Optional;

public interface UserRepository {

  Optional<User> findByEmail(String email);

  Optional<User> findById(UserId id);
}
