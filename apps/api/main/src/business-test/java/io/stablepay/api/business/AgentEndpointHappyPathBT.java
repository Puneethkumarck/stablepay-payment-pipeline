package io.stablepay.api.business;

import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_EVENT_TIME_1;
import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_EVENT_TIME_2;
import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_REFERENCE;
import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_TIMELINE_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.stablepay.api.application.web.dto.AgentSqlResponse;
import io.stablepay.api.application.web.dto.AgentTimelineResponse;
import io.stablepay.api.config.BusinessTestBase;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class AgentEndpointHappyPathBT extends BusinessTestBase {

  @AfterEach
  void tearDown() {
    stubRepositoryConfig.resetAgentStubs();
  }

  @Nested
  class SqlEndpoint {

    @Test
    void shouldReturnRowsForValidSelectQuery() {
      // given
      stubRepositoryConfig.setSqlExecutor(
          (sql, limit) ->
              new SqlExecutionResult.Success(
                  List.of("currency", "total"),
                  List.of(List.of("USD", 12500)),
                  1,
                  "q-bt-001",
                  42L));
      var client = authenticatedClient(agentJwt());
      var body =
          """
          {"sql": "SELECT currency, total FROM iceberg.analytics.v_payment_summary"}
          """;

      // when
      var response =
          client
              .post()
              .uri("/api/v1/agent/sql")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .toEntity(AgentSqlResponse.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      var expected =
          AgentSqlResponse.builder()
              .columns(List.of("currency", "total"))
              .rows(List.of(List.of("USD", 12500)))
              .rowCount(1)
              .queryId("q-bt-001")
              .tookMs(42L)
              .build();
      assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class SearchEndpoint {

    @Test
    void shouldReturnHitsForValidBoolTermSearch() {
      // given
      stubRepositoryConfig.setSearchExecutor(
          (request, size) ->
              new io.stablepay.api.domain.agent.SearchExecutionResult.Success(
                  List.of(Map.of("event_id", "e1", "customer_id", "cust-001")), 1L, size, 15L));
      var client = authenticatedClient(agentJwt());
      var body =
          """
          {
            "index": "transactions",
            "query": {"type": "term", "field": "customer_id", "value": "cust-001"}
          }
          """;

      // when
      var response =
          client
              .post()
              .uri("/api/v1/agent/search")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .toEntity(String.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).contains("event_id");
      assertThat(response.getBody()).contains("cust-001");
    }
  }

  @Nested
  class TimelineEndpoint {

    @Test
    void shouldReturnTimelineForExistingReference() {
      // given
      stubRepositoryConfig.setTimelineLookup(
          reference -> {
            if (SOME_REFERENCE.equals(reference)) {
              return SOME_TIMELINE_ENTRIES;
            }
            return List.of();
          });
      var client = authenticatedClient(agentJwt());

      // when
      var response =
          client
              .get()
              .uri("/api/v1/agent/timeline/{ref}", SOME_REFERENCE)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .toEntity(AgentTimelineResponse.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      var body = response.getBody();
      assertThat(body).isNotNull();
      assertThat(body.reference()).isEqualTo(SOME_REFERENCE);
      assertThat(body.markdown()).contains("# Timeline for " + SOME_REFERENCE);
      assertThat(body.entries()).hasSize(2);
      assertThat(body.entries().getFirst().eventId())
          .isEqualTo("evt-aabbccdd-1111-2222-3333-444444444444");
      assertThat(body.entries().getFirst().timestamp()).isEqualTo(SOME_EVENT_TIME_1);
      assertThat(body.entries().getLast().timestamp()).isEqualTo(SOME_EVENT_TIME_2);
    }

    @Test
    void shouldReturn404ForMissingReference() {
      // given
      var client = authenticatedClient(agentJwt());

      // when/then
      var exception =
          catchThrowableOfType(
              HttpClientErrorException.class,
              () ->
                  client
                      .get()
                      .uri("/api/v1/agent/timeline/{ref}", "nonexistent-ref")
                      .accept(MediaType.APPLICATION_JSON)
                      .retrieve()
                      .toEntity(String.class));
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(exception.getResponseBodyAsString()).contains("STBLPAY-3001");
    }
  }
}
