package io.stablepay.api.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.stablepay.api.config.BusinessTestBase;
import io.stablepay.api.config.StubRepositoryConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class AgentRateLimitBT extends BusinessTestBase {

  private static final UUID RATE_LIMIT_AGENT_UUID =
      UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

  @Test
  void shouldRejectAgentAfterBucketExhausted() {
    // given — use a dedicated UUID so other agent tests don't deplete the bucket
    var jwt = mintJwt(RATE_LIMIT_AGENT_UUID, "rate-agent@stablepay.io", List.of("AGENT"), null);
    var client = authenticatedClient(jwt);
    var body =
        """
        {"sql": "SELECT * FROM iceberg.analytics.v_payment_summary"}
        """;

    // when
    for (var i = 0; i < StubRepositoryConfig.BT_RATE_LIMIT_CAPACITY; i++) {
      var ok =
          client
              .post()
              .uri("/api/v1/agent/sql")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .toEntity(String.class);
      assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // then
    var exception =
        catchThrowableOfType(
            HttpClientErrorException.class,
            () ->
                client
                    .post()
                    .uri("/api/v1/agent/sql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exception.getResponseHeaders()).isNotNull();
    assertThat(exception.getResponseHeaders().getFirst("Retry-After")).isNotNull();
    assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-4001");
  }
}
