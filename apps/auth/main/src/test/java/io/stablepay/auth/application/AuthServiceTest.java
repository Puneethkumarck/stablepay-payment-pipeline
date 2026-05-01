package io.stablepay.auth.application;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_EMAIL;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_LATER_INSTANT;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_PASSWORD_HASH;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.activeRefreshToken;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.adminUser;
import static io.stablepay.auth.domain.model.AuthDomainFixtures.customerUser;
import static io.stablepay.auth.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.RefreshTokenRepository;
import io.stablepay.auth.domain.port.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String ISSUER = "https://auth.stablepay.local";
  private static final String AUDIENCE = "stablepay-api";
  private static final String SOME_KID = "test-kid";
  private static final Clock FIXED_CLOCK = Clock.fixed(SOME_INSTANT, ZoneOffset.UTC);

  private static RSAPrivateKey privateKey;
  private static RSAPublicKey publicKey;
  private static RSASSASigner signer;

  @Mock private UserRepository users;
  @Mock private RefreshTokenRepository tokens;
  @Mock private SigningKeyManager keys;
  @Mock private PasswordHasher hasher;

  private AuthService service;

  @BeforeAll
  static void generateKeypair() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    privateKey = (RSAPrivateKey) pair.getPrivate();
    publicKey = (RSAPublicKey) pair.getPublic();
    signer = new RSASSASigner(privateKey);
  }

  @BeforeEach
  void setUp() {
    service = new AuthService(users, tokens, keys, hasher, FIXED_CLOCK);
    ReflectionTestUtils.setField(service, "issuer", ISSUER);
    ReflectionTestUtils.setField(service, "audience", AUDIENCE);
  }

  @Test
  void loginReturnsSuccessForValidCredentials() throws Exception {
    // given
    var user = customerUser();
    given(users.findByEmail(SOME_EMAIL)).willReturn(Optional.of(user));
    given(hasher.matches("demo1234", SOME_PASSWORD_HASH)).willReturn(true);
    givenSigningKey();
    var expectedClaims =
        new JWTClaimsSet.Builder()
            .subject(user.id().value().toString())
            .claim("email", SOME_EMAIL)
            .claim("roles", List.of("CUSTOMER"))
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .issueTime(Date.from(SOME_INSTANT))
            .expirationTime(Date.from(SOME_INSTANT.plus(Duration.ofMinutes(15))))
            .claim("customer_id", user.customerId().orElseThrow().value().toString())
            .build();

    // when
    var actual = service.login(SOME_EMAIL, "demo1234", SOME_INSTANT);

    // then
    assertThat(actual).isInstanceOf(AuthService.LoginOutcome.Success.class);
    var success = (AuthService.LoginOutcome.Success) actual;
    assertThat(success.expiresIn()).isEqualTo(Duration.ofMinutes(15));
    var actualClaims = verifyAndExtractClaims(success.accessToken());
    assertThat(actualClaims.toJSONObject())
        .usingRecursiveComparison()
        .ignoringFields("jti")
        .isEqualTo(expectedClaims.toJSONObject());
    assertThat(actualClaims.getJWTID()).isNotBlank();
    var expectedRefreshToken =
        RefreshToken.builder()
            .id(activeRefreshToken().id())
            .userId(user.id())
            .tokenHash(sha256(success.refreshToken()))
            .issuedAt(SOME_INSTANT)
            .expiresAt(SOME_INSTANT.plus(Duration.ofDays(7)))
            .revokedAt(Optional.empty())
            .build();
    then(tokens).should().save(eqIgnoring(expectedRefreshToken, "id"));
  }

  @Test
  void loginIncludesNoCustomerIdClaimForAdminUser() throws Exception {
    // given
    var admin = adminUser();
    given(users.findByEmail("admin@stablepay.io")).willReturn(Optional.of(admin));
    given(hasher.matches("demo1234", SOME_PASSWORD_HASH)).willReturn(true);
    givenSigningKey();

    // when
    var actual = service.login("admin@stablepay.io", "demo1234", SOME_INSTANT);

    // then
    var success = (AuthService.LoginOutcome.Success) actual;
    var claims = verifyAndExtractClaims(success.accessToken());
    assertThat(claims.getClaim("customer_id")).isNull();
  }

  @Test
  void loginReturnsInvalidCredentialsForWrongPassword() {
    // given
    var user = customerUser();
    given(users.findByEmail(SOME_EMAIL)).willReturn(Optional.of(user));
    given(hasher.matches("wrong", SOME_PASSWORD_HASH)).willReturn(false);

    // when
    var actual = service.login(SOME_EMAIL, "wrong", SOME_INSTANT);

    // then
    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(new AuthService.LoginOutcome.InvalidCredentials());
    then(tokens).shouldHaveNoInteractions();
  }

  @Test
  void loginRunsBcryptEvenWhenUserNotFoundToMitigateTimingAttack() {
    // given
    given(users.findByEmail("ghost@stablepay.io")).willReturn(Optional.empty());
    given(hasher.dummyHash()).willReturn("dummy-hash");
    given(hasher.matches("any-password", "dummy-hash")).willReturn(false);

    // when
    var actual = service.login("ghost@stablepay.io", "any-password", SOME_INSTANT);

    // then
    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(new AuthService.LoginOutcome.InvalidCredentials());
    then(hasher).should().matches("any-password", "dummy-hash");
    then(tokens).shouldHaveNoInteractions();
  }

  @Test
  void refreshRotatesToken() throws Exception {
    // given
    var user = customerUser();
    var existing = activeRefreshToken();
    var refreshTokenPlain = "old-refresh-token";
    given(tokens.findActiveByHash(sha256(refreshTokenPlain), SOME_LATER_INSTANT))
        .willReturn(Optional.of(existing));
    given(users.findById(user.id())).willReturn(Optional.of(user));
    givenSigningKey();
    var expectedNewToken =
        RefreshToken.builder()
            .id(existing.id())
            .userId(user.id())
            .tokenHash("ignored")
            .issuedAt(SOME_LATER_INSTANT)
            .expiresAt(SOME_LATER_INSTANT.plus(Duration.ofDays(7)))
            .revokedAt(Optional.empty())
            .build();

    // when
    var actual = service.refresh(refreshTokenPlain, SOME_LATER_INSTANT);

    // then
    assertThat(actual).isInstanceOf(AuthService.LoginOutcome.Success.class);
    then(tokens).should().revoke(existing.id(), SOME_LATER_INSTANT);
    then(tokens).should().save(eqIgnoring(expectedNewToken, "id", "tokenHash"));
  }

  @Test
  void refreshReturnsInvalidCredentialsWhenTokenNotFound() throws Exception {
    // given
    given(tokens.findActiveByHash(sha256("missing"), SOME_LATER_INSTANT))
        .willReturn(Optional.empty());

    // when
    var actual = service.refresh("missing", SOME_LATER_INSTANT);

    // then
    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(new AuthService.LoginOutcome.InvalidCredentials());
    then(tokens).should().findActiveByHash(sha256("missing"), SOME_LATER_INSTANT);
    then(tokens).shouldHaveNoMoreInteractions();
  }

  @Test
  void refreshReturnsInvalidCredentialsWhenUserNoLongerExists() throws Exception {
    // given
    var existing = activeRefreshToken();
    var refreshTokenPlain = "orphan-token";
    given(tokens.findActiveByHash(sha256(refreshTokenPlain), SOME_LATER_INSTANT))
        .willReturn(Optional.of(existing));
    given(users.findById(existing.userId())).willReturn(Optional.empty());

    // when
    var actual = service.refresh(refreshTokenPlain, SOME_LATER_INSTANT);

    // then
    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(new AuthService.LoginOutcome.InvalidCredentials());
    then(tokens).should().findActiveByHash(sha256(refreshTokenPlain), SOME_LATER_INSTANT);
    then(tokens).shouldHaveNoMoreInteractions();
  }

  @Test
  void logoutRevokesActiveRefreshToken() throws Exception {
    // given
    var existing = activeRefreshToken();
    var refreshTokenPlain = "active-token";
    given(tokens.findActiveByHash(sha256(refreshTokenPlain), SOME_LATER_INSTANT))
        .willReturn(Optional.of(existing));

    // when
    service.logout(refreshTokenPlain, SOME_LATER_INSTANT);

    // then
    then(tokens).should().revoke(existing.id(), SOME_LATER_INSTANT);
  }

  @Test
  void logoutIsNoOpWhenTokenNotActive() throws Exception {
    // given
    given(tokens.findActiveByHash(sha256("stale"), SOME_LATER_INSTANT))
        .willReturn(Optional.empty());

    // when
    service.logout("stale", SOME_LATER_INSTANT);

    // then
    then(tokens).should().findActiveByHash(sha256("stale"), SOME_LATER_INSTANT);
    then(tokens).shouldHaveNoMoreInteractions();
  }

  @Test
  void cleanupDeletesRefreshTokensOlderThanThirtyDays() {
    // given
    var cutoff = SOME_INSTANT.minus(Duration.ofDays(30));
    given(tokens.deleteRevokedAndExpiredOlderThan(cutoff)).willReturn(7);

    // when
    service.cleanupExpiredRefreshTokens();

    // then
    then(tokens).should().deleteRevokedAndExpiredOlderThan(cutoff);
  }

  private void givenSigningKey() {
    var key =
        SigningKey.builder()
            .kid(SOME_KID)
            .privateKeyPem("ignored")
            .publicKeyPem("ignored")
            .algorithm("RS256")
            .createdAt(SOME_INSTANT)
            .isActive(true)
            .build();
    given(keys.getActiveKey()).willReturn(key);
    given(keys.getActiveSigner()).willReturn(signer);
  }

  private static JWTClaimsSet verifyAndExtractClaims(String jwt) throws Exception {
    var parsed = SignedJWT.parse(jwt);
    assertThat(parsed.getHeader().getKeyID()).isEqualTo(SOME_KID);
    assertThat(parsed.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
    assertThat(parsed.verify(new RSASSAVerifier(publicKey))).isTrue();
    return parsed.getJWTClaimsSet();
  }

  private static String sha256(String input) throws Exception {
    var digest =
        MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }
}
