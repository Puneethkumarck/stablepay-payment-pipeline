package io.stablepay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import io.stablepay.auth.client.ApiError;
import io.stablepay.auth.infrastructure.ratelimit.LoginRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = StablepayAuthApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthRateLimitIT extends AbstractAuthBusinessTest {

  @Test
  void sixthRapidLoginAttemptFromSameIpReturns429WithRetryAfter() {
    // given — a fresh Caffeine bucket (forced by @DirtiesContext on this class)

    // when — drain the bucket with five rapid bad-credential logins
    for (var i = 0; i < LoginRateLimitFilter.BUCKET_CAPACITY; i++) {
      var drainResponse = postLoginExpectingError("alice@stablepay.io", "wrong-password-" + i);
      assertThat(drainResponse.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    // then — the next login is rate-limited, surfaces the documented error code,
    // and includes the Retry-After header per the rate-limit filter contract
    var rateLimited = postLoginExpectingError("alice@stablepay.io", "wrong-password-final");
    assertThat(rateLimited.getStatusCode()).isEqualTo(TOO_MANY_REQUESTS);
    assertThat(rateLimited.getHeaders().getFirst(RETRY_AFTER))
        .isEqualTo(String.valueOf(LoginRateLimitFilter.RETRY_AFTER_SECONDS));
    var expected =
        new ApiError(
            LoginRateLimitFilter.RATE_LIMIT_ERROR_CODE,
            LoginRateLimitFilter.RATE_LIMIT_MESSAGE,
            null);
    assertThat(rateLimited.getBody())
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(expected);
  }
}
