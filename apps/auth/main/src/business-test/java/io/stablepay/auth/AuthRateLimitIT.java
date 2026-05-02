package io.stablepay.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = StablepayAuthApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthRateLimitIT extends AbstractAuthBusinessTest {

  private static final int BUCKET_CAPACITY = 5;
  private static final long EXPECTED_RETRY_AFTER_SECONDS = 60L;

  @LocalServerPort private int port;
  @Autowired private ObjectMapper objectMapper;
  private final TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  void sixthRapidLoginAttemptFromSameIpReturns429WithRetryAfter() throws Exception {
    // given — a fresh Caffeine bucket (forced by @DirtiesContext on this class)

    // when — drain the bucket with five rapid bad-credential logins from 127.0.0.1
    for (var i = 0; i < BUCKET_CAPACITY; i++) {
      var drainResponse = postLogin("alice@stablepay.io", "wrong-password-" + i);
      assertThat(drainResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // then — the sixth login is rate-limited, surfaces the documented error code,
    // and includes the Retry-After header per the rate-limit filter contract
    var rateLimited = postLogin("alice@stablepay.io", "wrong-password-final");
    assertThat(rateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(rateLimited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        .isEqualTo(String.valueOf(EXPECTED_RETRY_AFTER_SECONDS));
    var actual = parseError(rateLimited.getBody());
    var expected =
        new ApiErrorView(
            "STBLPAY-1004",
            "Too many login attempts. Try again in " + EXPECTED_RETRY_AFTER_SECONDS + " seconds.");
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  private ResponseEntity<String> postLogin(String email, String password) {
    var body = Map.of("email", email, "password", password);
    return restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/auth/login",
        HttpMethod.POST,
        jsonEntity(body),
        String.class);
  }

  private HttpEntity<Map<String, ?>> jsonEntity(Map<String, ?> body) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  private ApiErrorView parseError(String body) {
    var node = objectMapper.readTree(body);
    return new ApiErrorView(node.get("error_code").asString(), node.get("message").asString());
  }

  private record ApiErrorView(String errorCode, String message) {}
}
