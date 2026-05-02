package io.stablepay.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.stablepay.auth.application.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = StablepayAuthApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIT extends AbstractAuthBusinessTest {

  private static final String ALICE_USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final String ALICE_CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";
  private static final String ADMIN_USER_ID = "33333333-3333-3333-3333-333333333333";
  private static final String ALICE_EMAIL = "alice@stablepay.io";
  private static final String ADMIN_EMAIL = "admin@stablepay.io";
  private static final String SEEDED_PASSWORD = "demo1234";
  private static final String ISSUER = "https://auth.stablepay.local";
  private static final String AUDIENCE = "stablepay-api";

  @LocalServerPort private int port;
  @Autowired private ObjectMapper objectMapper;
  private final TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  void aliceCompletesFullLoginRefreshAndLogoutLifecycle() throws Exception {
    // given — issuer is up, alice is seeded, JWKS is published

    // when — step 1: alice logs in
    var loginResponse = postLogin(ALICE_EMAIL, SEEDED_PASSWORD);

    // then — 200 OK + tokens with the documented TTL
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    var loginTokens = parseTokens(loginResponse.getBody());
    var tokenShape =
        new TokenShape(
            !loginTokens.accessToken().isBlank(),
            !loginTokens.refreshToken().isBlank(),
            loginTokens.expiresIn());
    assertThat(tokenShape)
        .usingRecursiveComparison()
        .isEqualTo(new TokenShape(true, true, AuthService.ACCESS_TTL.toSeconds()));

    // when — step 2 + 4: decode the access token and verify its claims
    var decodedAccessToken = SignedJWT.parse(loginTokens.accessToken());
    var actualClaims = JwtClaimsView.from(decodedAccessToken);
    var expectedClaims =
        new JwtClaimsView(
            ALICE_USER_ID,
            ALICE_EMAIL,
            List.of("ADMIN", "CUSTOMER"),
            ISSUER,
            List.of(AUDIENCE),
            Optional.of(ALICE_CUSTOMER_ID));
    assertThat(actualClaims).usingRecursiveComparison().isEqualTo(expectedClaims);

    // when — step 3 + 4: fetch the JWKS and verify the access-token signature
    var jwksResponse = restTemplate.getForEntity(url("/.well-known/jwks.json"), String.class);
    assertThat(jwksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    var jwkSet = JWKSet.parse(jwksResponse.getBody());
    var signingJwk = (RSAKey) jwkSet.getKeyByKeyId(decodedAccessToken.getHeader().getKeyID());
    assertThat(signingJwk).isNotNull();
    var verifier = new RSASSAVerifier(signingJwk.toRSAPublicKey());
    assertThat(decodedAccessToken.verify(verifier)).isTrue();

    // when — step 5: refresh rotates the tokens
    var refreshResponse = postRefresh(loginTokens.refreshToken());
    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    var rotatedTokens = parseTokens(refreshResponse.getBody());
    var rotation =
        new RotationShape(
            !rotatedTokens.accessToken().equals(loginTokens.accessToken()),
            !rotatedTokens.refreshToken().equals(loginTokens.refreshToken()));
    assertThat(rotation).usingRecursiveComparison().isEqualTo(new RotationShape(true, true));

    // and — invariant: the OLD refresh token is rejected on a subsequent /refresh call
    var replayedOldRefresh = postRefresh(loginTokens.refreshToken());
    assertThat(replayedOldRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // when — step 6: logout the rotated refresh token
    var logoutResponse = postLogout(rotatedTokens.refreshToken());
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // and — invariant: logout is idempotent (second call also returns 204)
    var logoutIdempotentResponse = postLogout(rotatedTokens.refreshToken());
    assertThat(logoutIdempotentResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // and — invariant: the previously-issued access token remains cryptographically valid
    // for the remainder of its 15-minute TTL (auth service has no access-token revocation
    // list — verified by checking signature still verifies and exp is in the future).
    var postLogoutAccessToken = SignedJWT.parse(rotatedTokens.accessToken());
    var postLogoutValidity =
        new AccessTokenValidity(
            postLogoutAccessToken.verify(verifier),
            postLogoutAccessToken.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(now()));
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
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    var tokens = parseTokens(loginResponse.getBody());
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
  void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
    // given

    // when
    var response = postLogin(ALICE_EMAIL, "wrong-password");

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    var actualError = parseError(response.getBody());
    var expectedError = new ApiErrorView("STBLPAY-1001", "Invalid credentials");
    assertThat(actualError).usingRecursiveComparison().isEqualTo(expectedError);
  }

  private ResponseEntity<String> postLogin(String email, String password) {
    var body = Map.of("email", email, "password", password);
    return restTemplate.exchange(
        url("/api/v1/auth/login"), HttpMethod.POST, jsonEntity(body), String.class);
  }

  private ResponseEntity<String> postRefresh(String refreshToken) {
    var body = Map.of("refresh_token", refreshToken);
    return restTemplate.exchange(
        url("/api/v1/auth/refresh"), HttpMethod.POST, jsonEntity(body), String.class);
  }

  private ResponseEntity<Void> postLogout(String refreshToken) {
    var body = Map.of("refresh_token", refreshToken);
    return restTemplate.exchange(
        url("/api/v1/auth/logout"), HttpMethod.POST, jsonEntity(body), Void.class);
  }

  private HttpEntity<Map<String, ?>> jsonEntity(Map<String, ?> body) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  private TokenView parseTokens(String body) {
    var node = objectMapper.readTree(body);
    return new TokenView(
        node.get("access_token").asString(),
        node.get("refresh_token").asString(),
        node.get("expires_in").asLong());
  }

  private ApiErrorView parseError(String body) {
    var node = objectMapper.readTree(body);
    return new ApiErrorView(node.get("error_code").asString(), node.get("message").asString());
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private static Instant now() {
    return Instant.now();
  }

  private record TokenView(String accessToken, String refreshToken, long expiresIn) {}

  private record TokenShape(boolean hasAccessToken, boolean hasRefreshToken, long expiresIn) {}

  private record RotationShape(boolean accessTokenRotated, boolean refreshTokenRotated) {}

  private record AccessTokenValidity(boolean signatureValid, boolean notExpired) {}

  private record ApiErrorView(String errorCode, String message) {}

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
