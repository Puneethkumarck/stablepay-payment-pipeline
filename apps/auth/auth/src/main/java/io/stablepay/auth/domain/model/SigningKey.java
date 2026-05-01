package io.stablepay.auth.domain.model;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record SigningKey(
    String kid,
    String privateKeyPem,
    String publicKeyPem,
    String algorithm,
    Instant createdAt,
    boolean isActive) {}
