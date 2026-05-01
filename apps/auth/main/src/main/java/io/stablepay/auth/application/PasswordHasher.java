package io.stablepay.auth.application;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

  // Computed once at class load. Recomputing per call would add a second BCrypt op
  // to the unknown-user path and leak account existence via response timing.
  private static final String DUMMY_HASH = ENCODER.encode("invalid");

  public String hash(String plaintext) {
    return ENCODER.encode(plaintext);
  }

  public boolean matches(String plaintext, String hash) {
    return ENCODER.matches(plaintext, hash);
  }

  public String dummyHash() {
    return DUMMY_HASH;
  }
}
