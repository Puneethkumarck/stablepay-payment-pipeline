package io.stablepay.auth.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AuthDomainFixtures {

  public static final UUID SOME_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID SOME_CUSTOMER_UUID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  public static final UUID SOME_REFRESH_TOKEN_UUID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  public static final UserId SOME_USER_ID = UserId.of(SOME_USER_UUID);
  public static final CustomerId SOME_CUSTOMER_ID = CustomerId.of(SOME_CUSTOMER_UUID);
  public static final RefreshTokenId SOME_REFRESH_TOKEN_ID =
      RefreshTokenId.of(SOME_REFRESH_TOKEN_UUID);
  public static final String SOME_EMAIL = "alice@stablepay.io";
  public static final String SOME_PASSWORD_HASH = "fake-bcrypt-hash";
  public static final String SOME_TOKEN_HASH = "fake-token-hash";
  public static final String SOME_KID = "key-2026-05-01";
  public static final String SOME_PRIVATE_KEY_PEM = "fake-private-key-pem";
  public static final String SOME_PUBLIC_KEY_PEM = "fake-public-key-pem";
  public static final String SOME_ALGORITHM = "RS256";
  public static final Instant SOME_INSTANT = Instant.parse("2026-05-01T10:00:00Z");
  public static final Instant SOME_LATER_INSTANT = Instant.parse("2026-05-01T11:00:00Z");
  public static final Instant SOME_EXPIRES_AT = Instant.parse("2026-05-08T10:00:00Z");

  private AuthDomainFixtures() {}

  public static User customerUser() {
    return User.builder()
        .id(SOME_USER_ID)
        .customerId(Optional.of(SOME_CUSTOMER_ID))
        .email(SOME_EMAIL)
        .passwordHash(SOME_PASSWORD_HASH)
        .roles(Set.of(Role.CUSTOMER))
        .createdAt(SOME_INSTANT)
        .updatedAt(SOME_INSTANT)
        .build();
  }

  public static User adminUser() {
    return User.builder()
        .id(SOME_USER_ID)
        .customerId(Optional.empty())
        .email("admin@stablepay.io")
        .passwordHash(SOME_PASSWORD_HASH)
        .roles(Set.of(Role.ADMIN))
        .createdAt(SOME_INSTANT)
        .updatedAt(SOME_INSTANT)
        .build();
  }

  public static RefreshToken activeRefreshToken() {
    return RefreshToken.builder()
        .id(SOME_REFRESH_TOKEN_ID)
        .userId(SOME_USER_ID)
        .tokenHash(SOME_TOKEN_HASH)
        .issuedAt(SOME_INSTANT)
        .expiresAt(SOME_EXPIRES_AT)
        .revokedAt(Optional.empty())
        .build();
  }

  public static SigningKey activeSigningKey() {
    return SigningKey.builder()
        .kid(SOME_KID)
        .privateKeyPem(SOME_PRIVATE_KEY_PEM)
        .publicKeyPem(SOME_PUBLIC_KEY_PEM)
        .algorithm(SOME_ALGORITHM)
        .createdAt(SOME_INSTANT)
        .isActive(true)
        .build();
  }
}
