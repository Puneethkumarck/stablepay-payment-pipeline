package io.stablepay.api.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.stablepay.api.config.BusinessTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class AgentEndpointSecurityBT extends BusinessTestBase {

  @Nested
  class SqlAllowlistEnforcement {

    @Test
    void shouldRejectDeleteSqlWith400() {
      // given
      var client = authenticatedClient(agentJwt());
      var body =
          """
          {"sql": "DELETE FROM iceberg.analytics.v_payment_summary"}
          """;

      // when/then
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
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-5002");
    }
  }

  @Nested
  class DslWhitelistEnforcement {

    @Test
    void shouldRejectNonWhitelistedFieldWith400() {
      // given
      var client = authenticatedClient(agentJwt());
      var body =
          """
          {
            "index": "transactions",
            "query": {"type":"term", "field": "password_hash", "value": "secret"}
          }
          """;

      // when/then
      var exception =
          catchThrowableOfType(
              HttpClientErrorException.class,
              () ->
                  client
                      .post()
                      .uri("/api/v1/agent/search")
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(body)
                      .retrieve()
                      .toEntity(String.class));
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-6002");
    }
  }

  @Nested
  class RoleEnforcement {

    @Test
    void shouldRejectNonAgentUserWith403() {
      // given
      var client = authenticatedClient(aliceJwt());
      var body =
          """
          {"sql": "SELECT * FROM iceberg.analytics.v_payment_summary"}
          """;

      // when/then
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
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }
}
