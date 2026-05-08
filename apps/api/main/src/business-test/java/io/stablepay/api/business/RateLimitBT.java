package io.stablepay.api.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.stablepay.api.config.BusinessTestBase;
import io.stablepay.api.config.StubRepositoryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class RateLimitBT extends BusinessTestBase {

  @Test
  void customerShouldBeRateLimitedAfterBucketExhausted() {
    // given
    var jwt = aliceJwt();
    var client = authenticatedClient(jwt);
    var endpoint = "/api/v1/transactions";

    // when
    for (var i = 0; i < StubRepositoryConfig.BT_RATE_LIMIT_CAPACITY; i++) {
      client
          .get()
          .uri(endpoint)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .toEntity(String.class);
    }

    // then
    var exception =
        catchThrowableOfType(
            HttpClientErrorException.class,
            () ->
                client
                    .get()
                    .uri(endpoint)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exception.getResponseHeaders()).isNotNull();
    assertThat(exception.getResponseHeaders().getFirst("Retry-After")).isNotNull();
    assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-4001");
  }
}
