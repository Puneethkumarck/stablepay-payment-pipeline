package io.stablepay.api.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.stablepay.api.config.BusinessTestBase;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;

class AgentTimeoutBT extends BusinessTestBase {

  private static final UUID TIMEOUT_AGENT_UUID =
      UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

  @AfterEach
  void tearDown() {
    stubRepositoryConfig.resetAgentStubs();
  }

  @Test
  void shouldReturn503WhenSqlExecutionTimesOut() {
    // given
    stubRepositoryConfig.setSqlExecutor(
        (sql, limit) ->
            new SqlExecutionResult.Failed("STBLPAY-5005", "Query execution failed: timeout"));
    var jwt = mintJwt(TIMEOUT_AGENT_UUID, "timeout-agent@stablepay.io", List.of("AGENT"), null);
    var client = authenticatedClient(jwt);
    var body =
        """
        {"sql": "SELECT * FROM iceberg.analytics.v_payment_summary"}
        """;

    // when/then
    var exception =
        catchThrowableOfType(
            HttpServerErrorException.class,
            () ->
                client
                    .post()
                    .uri("/api/v1/agent/sql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-5005");
  }
}
