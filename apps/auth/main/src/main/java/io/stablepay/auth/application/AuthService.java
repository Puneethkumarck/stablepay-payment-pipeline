package io.stablepay.auth.application;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.RefreshTokenId;
import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.port.RefreshTokenRepository;
import io.stablepay.auth.domain.port.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  public static final Duration ACCESS_TTL = Duration.ofMinutes(15);
  public static final Duration REFRESH_TTL = Duration.ofDays(7);
  private static final Duration REFRESH_RETENTION = Duration.ofDays(30);
  private static final int REFRESH_TOKEN_BYTES = 32;
  private static final SecureRandom RNG = new SecureRandom();

  private final UserRepository users;
  private final RefreshTokenRepository tokens;
  private final SigningKeyManager keys;
  private final PasswordHasher hasher;
  private final Clock clock;

  @Value("${stablepay.auth.jwt.issuer:https://auth.stablepay.local}")
  private String issuer;

  @Value("${stablepay.auth.jwt.audience:stablepay-api}")
  private String audience;

  public sealed interface LoginOutcome {
    record Success(String accessToken, String refreshToken, Duration expiresIn)
        implements LoginOutcome {}

    record InvalidCredentials() implements LoginOutcome {}
  }

  @Transactional
  public LoginOutcome login(String email, String password, Instant now) {
    var maybeUser = users.findByEmail(email);
    var passwordOk =
        maybeUser
            .map(u -> hasher.matches(password, u.passwordHash()))
            .orElseGet(
                () -> {
                  hasher.matches(password, hasher.dummyHash());
                  return false;
                });
    if (!passwordOk) {
      return new LoginOutcome.InvalidCredentials();
    }
    var user = maybeUser.orElseThrow();
    return new LoginOutcome.Success(
        issueAccessToken(user, now), issueRefreshToken(user, now), ACCESS_TTL);
  }

  @Transactional
  public LoginOutcome refresh(String refreshToken, Instant now) {
    var existing = tokens.findActiveByHash(sha256(refreshToken), now);
    if (existing.isEmpty()) {
      return new LoginOutcome.InvalidCredentials();
    }
    var rt = existing.orElseThrow();
    var user = users.findById(rt.userId()).orElseThrow();
    tokens.revoke(rt.id(), now);
    return new LoginOutcome.Success(
        issueAccessToken(user, now), issueRefreshToken(user, now), ACCESS_TTL);
  }

  @Transactional
  public void logout(String refreshToken, Instant now) {
    tokens.findActiveByHash(sha256(refreshToken), now).ifPresent(rt -> tokens.revoke(rt.id(), now));
  }

  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void cleanupExpiredRefreshTokens() {
    var deleted = tokens.deleteRevokedAndExpiredOlderThan(clock.instant().minus(REFRESH_RETENTION));
    log.info("Deleted {} expired or revoked refresh tokens", deleted);
  }

  private String issueAccessToken(User user, Instant now) {
    try {
      var key = keys.getActiveKey();
      var claimsBuilder =
          new JWTClaimsSet.Builder()
              .subject(user.id().value().toString())
              .claim("email", user.email())
              .claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
              .issuer(issuer)
              .audience(audience)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(ACCESS_TTL)))
              .jwtID(UUID.randomUUID().toString());
      user.customerId()
          .ifPresent(cid -> claimsBuilder.claim("customer_id", cid.value().toString()));
      var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.kid()).build();
      var jwt = new SignedJWT(header, claimsBuilder.build());
      jwt.sign(keys.getActiveSigner());
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign access token", e);
    }
  }

  private String issueRefreshToken(User user, Instant now) {
    var bytes = new byte[REFRESH_TOKEN_BYTES];
    RNG.nextBytes(bytes);
    var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var stored =
        RefreshToken.builder()
            .id(RefreshTokenId.of(UUID.randomUUID()))
            .userId(user.id())
            .tokenHash(sha256(token))
            .issuedAt(now)
            .expiresAt(now.plus(REFRESH_TTL))
            .revokedAt(Optional.empty())
            .build();
    tokens.save(stored);
    return token;
  }

  private static String sha256(String input) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
