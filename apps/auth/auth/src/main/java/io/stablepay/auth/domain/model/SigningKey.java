package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record SigningKey(
    String kid,
    String privateKeyPem,
    String publicKeyPem,
    String algorithm,
    Instant createdAt,
    boolean isActive) {

  public SigningKey {
    Objects.requireNonNull(kid, "kid");
    Objects.requireNonNull(privateKeyPem, "privateKeyPem");
    Objects.requireNonNull(publicKeyPem, "publicKeyPem");
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  @Override
  public String toString() {
    return "SigningKey[kid="
        + kid
        + ", privateKeyPem=***REDACTED***"
        + ", publicKeyPem="
        + publicKeyPem
        + ", algorithm="
        + algorithm
        + ", createdAt="
        + createdAt
        + ", isActive="
        + isActive
        + "]";
  }
}
