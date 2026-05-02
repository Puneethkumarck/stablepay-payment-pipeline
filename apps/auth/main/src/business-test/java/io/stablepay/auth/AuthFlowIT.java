package io.stablepay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.stablepay.auth.application.AuthService;
import io.stablepay.auth.client.ApiError;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = StablepayAuthApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthFlowIT extends AbstractAuthBusinessTest {

  private static final String ALICE_ID = "11111111-1111-1111-1111-111111111111";
  private static final String ADMIN_USER_ID = "33333333-3333-3333-3333-333333333333";
  private static final String ALICE_EMAIL = "alice@stablepay.io";
  private static final String ADMIN_EMAIL = "admin@stablepay.io";
  private static final String SEEDED_PASSWORD = "demo1234";
  private static final String ISSUER = "https://auth.stablepay.local";
  private static final String AUDIENCE = "stablepay-api";

  @Autowired private Clock clock;

  @Test
  void aliceCompletesFullLoginRefreshAndLogoutLifecycle() throws Exception {
    // given — issuer is up, alice is seeded, JWKS is published

    // when — alice logs in
    var loginResponse = postLogin(ALICE_EMAIL, SEEDED_PASSWORD);

    // then — 200 OK + tokens with the documented TTL
    assertThat(loginResponse.getStatusCode()).isEqualTo(OK);
    var loginTokens = loginResponse.getBody();
    assertThat(loginTokens).isNotNull();
    var tokenShape =
        new TokenShape(
            !loginTokens.accessToken().isBlank(),
            !loginTokens.refreshToken().isBlank(),
            loginTokens.expiresIn());
    assertThat(tokenShape)
        .usingRecursiveComparison()
        .isEqualTo(new TokenShape(true, true, AuthService.ACCESS_TTL.toSeconds()));

    // and — the access token's claims match what alice was issued
    var decodedAccessToken = SignedJWT.parse(loginTokens.accessToken());
    var actualClaims = JwtClaimsView.from(decodedAccessToken);
    var expectedClaims =
        new JwtClaimsView(
            ALICE_ID,
            ALICE_EMAIL,
            List.of("ADMIN", "CUSTOMER"),
            ISSUER,
            List.of(AUDIENCE),
            Optional.of(ALICE_ID));
    assertThat(actualClaims).usingRecursiveComparison().isEqualTo(expectedClaims);

    // and — the JWKS publishes a key whose kid matches and verifies the signature
    var jwksResponse = restTemplate.getForEntity("/.well-known/jwks.json", String.class);
    assertThat(jwksResponse.getStatusCode()).isEqualTo(OK);
    var jwkSet = JWKSet.parse(jwksResponse.getBody());
    var signingJwk = (RSAKey) jwkSet.getKeyByKeyId(decodedAccessToken.getHeader().getKeyID());
    assertThat(signingJwk).isNotNull();
    var verifier = new RSASSAVerifier(signingJwk.toRSAPublicKey());
    assertThat(decodedAccessToken.verify(verifier)).isTrue();

    // and — refresh rotates both tokens
    var refreshResponse = postRefresh(loginTokens.refreshToken());
    assertThat(refreshResponse.getStatusCode()).isEqualTo(OK);
    var rotatedTokens = refreshResponse.getBody();
    assertThat(rotatedTokens).isNotNull();
    var rotation =
        new RotationShape(
            !rotatedTokens.accessToken().equals(loginTokens.accessToken()),
            !rotatedTokens.refreshToken().equals(loginTokens.refreshToken()));
    assertThat(rotation).usingRecursiveComparison().isEqualTo(new RotationShape(true, true));

    // and — invariant: the OLD refresh token is rejected on a subsequent /refresh call
    var replayedOldRefresh = postRefreshExpectingError(loginTokens.refreshToken());
    assertThat(replayedOldRefresh.getStatusCode()).isEqualTo(UNAUTHORIZED);

    // and — logout the rotated refresh token
    var logoutResponse = postLogout(rotatedTokens.refreshToken());
    assertThat(logoutResponse.getStatusCode()).isEqualTo(NO_CONTENT);

    // and — invariant: logout is idempotent (second call also returns 204)
    var logoutIdempotentResponse = postLogout(rotatedTokens.refreshToken());
    assertThat(logoutIdempotentResponse.getStatusCode()).isEqualTo(NO_CONTENT);

    // and — invariant: the previously-issued access token remains cryptographically valid
    // for the remainder of its 15-minute TTL (auth service has no access-token revocation
    // list — verified by checking signature still verifies and exp is in the future).
    var postLogoutAccessToken = SignedJWT.parse(rotatedTokens.accessToken());
    var postLogoutValidity =
        new AccessTokenValidity(
            postLogoutAccessToken.verify(verifier),
            postLogoutAccessToken
                .getJWTClaimsSet()
                .getExpirationTime()
                .toInstant()
                .isAfter(clock.instant()));
    assertThat(postLogoutValidity)
        .usingRecursiveComparison()
        .isEqualTo(new AccessTokenValidity(true, true));
  }

  @Test
  void adminAccessTokenExcludesCustomerIdClaim() throws Exception {
    // given — admin is seeded with NULL customer_id

    // when
    var loginResponse = postLogin(ADMIN_EMAIL, SEEDED_PASSWORD);

    // then
    assertThat(loginResponse.getStatusCode()).isEqualTo(OK);
    var tokens = loginResponse.getBody();
    assertThat(tokens).isNotNull();
    var actualClaims = JwtClaimsView.from(SignedJWT.parse(tokens.accessToken()));
    var expectedClaims =
        new JwtClaimsView(
            ADMIN_USER_ID,
            ADMIN_EMAIL,
            List.of("ADMIN"),
            ISSUER,
            List.of(AUDIENCE),
            Optional.empty());
    assertThat(actualClaims).usingRecursiveComparison().isEqualTo(expectedClaims);
  }

  @Test
  void loginWithWrongPasswordReturnsUnauthorized() {
    // when
    var response = postLoginExpectingError(ALICE_EMAIL, "wrong-password");

    // then
    assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    var expected = new ApiError("STBLPAY-1001", "Invalid credentials", null);
    assertThat(response.getBody())
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(expected);
  }

  // Test-only assertion shapes: the production API returns random tokens, so we cannot
  // recursive-compare against fixed values. These records package boolean invariants
  // ("token issued", "tokens rotated", "still valid") into a single object so each
  // assertion follows the golden recursive-comparison rule.
  private record TokenShape(boolean hasAccessToken, boolean hasRefreshToken, long expiresIn) {}

  private record RotationShape(boolean accessTokenRotated, boolean refreshTokenRotated) {}

  private record AccessTokenValidity(boolean signatureValid, boolean notExpired) {}

  private record JwtClaimsView(
      String subject,
      String email,
      List<String> roles,
      String issuer,
      List<String> audience,
      Optional<String> customerId) {

    static JwtClaimsView from(SignedJWT token) throws Exception {
      var claims = token.getJWTClaimsSet();
      var roles = claims.getStringListClaim("roles");
      var customerIdClaim = claims.getStringClaim("customer_id");
      return new JwtClaimsView(
          claims.getSubject(),
          claims.getStringClaim("email"),
          roles == null ? List.of() : List.copyOf(roles),
          claims.getIssuer(),
          claims.getAudience(),
          Optional.ofNullable(customerIdClaim));
    }
  }
}
