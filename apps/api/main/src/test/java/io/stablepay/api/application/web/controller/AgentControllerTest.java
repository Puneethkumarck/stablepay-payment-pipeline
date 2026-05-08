package io.stablepay.api.application.web.controller;

import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_REFERENCE;
import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_TIMELINE_ENTRIES;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validBoolTermRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.web.dto.AgentSearchResponse;
import io.stablepay.api.application.web.dto.AgentSqlRequest;
import io.stablepay.api.application.web.dto.AgentSqlResponse;
import io.stablepay.api.application.web.dto.AgentTimelineResponse;
import io.stablepay.api.application.web.dto.TimelineEntryDto;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.agent.AgentSearchService;
import io.stablepay.api.domain.agent.AgentSqlService;
import io.stablepay.api.domain.agent.AgentTimelineService;
import io.stablepay.api.domain.agent.FetchResult;
import io.stablepay.api.domain.agent.SearchExecutionResult;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import io.stablepay.api.domain.agent.Timeline;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-08T10:00:00Z");

  @Mock private AgentSqlService sqlService;
  @Mock private AgentSearchService searchService;
  @Mock private AgentTimelineService timelineService;
  @Spy private Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @InjectMocks private AgentController controller;

  @Nested
  class ExecuteSql {

    @Test
    void shouldReturn200OnSuccess() {
      // given
      var request = new AgentSqlRequest("SELECT 1", Optional.empty());
      var success =
          new SqlExecutionResult.Success(List.of("col1"), List.of(List.of(1)), 1, "q-1", 42L);
      given(sqlService.execute("SELECT 1", 1_000)).willReturn(success);

      // when
      var actual = controller.executeSql(request);

      // then
      var expected =
          ResponseEntity.ok(
              AgentSqlResponse.builder()
                  .columns(List.of("col1"))
                  .rows(List.of(List.of(1)))
                  .rowCount(1)
                  .queryId("q-1")
                  .tookMs(42L)
                  .build());
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn400OnRejected() {
      // given
      var request = new AgentSqlRequest("DROP TABLE x", Optional.empty());
      var rejected = new SqlExecutionResult.Rejected("STBLPAY-5002", "Only SELECT queries allowed");
      given(sqlService.execute("DROP TABLE x", 1_000)).willReturn(rejected);

      // when
      var actual = controller.executeSql(request);

      // then
      var expected =
          ResponseEntity.badRequest()
              .body(new ApiError("STBLPAY-5002", "Only SELECT queries allowed", FIXED_NOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn503OnFailed() {
      // given
      var request = new AgentSqlRequest("SELECT 1", Optional.of(100));
      var failed = new SqlExecutionResult.Failed("STBLPAY-5005", "Query execution failed");
      given(sqlService.execute("SELECT 1", 100)).willReturn(failed);

      // when
      var actual = controller.executeSql(request);

      // then
      var expected =
          ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body(new ApiError("STBLPAY-5005", "Query execution failed", FIXED_NOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class ExecuteSearch {

    @Test
    void shouldReturn200OnSuccess() {
      // given
      var request = validBoolTermRequest();
      var success =
          new SearchExecutionResult.Success(List.of(Map.of("event_id", "e1")), 1L, 50, 15L);
      given(searchService.execute(request)).willReturn(success);

      // when
      var actual = controller.executeSearch(request);

      // then
      var expected =
          ResponseEntity.ok(
              AgentSearchResponse.builder()
                  .hits(List.of(Map.of("event_id", "e1")))
                  .totalHits(1L)
                  .resolvedSize(50)
                  .tookMs(15L)
                  .build());
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn400OnRejected() {
      // given
      var request = validBoolTermRequest();
      var rejected =
          new SearchExecutionResult.Rejected("STBLPAY-6001", "Index not in allowlist: secret");
      given(searchService.execute(request)).willReturn(rejected);

      // when
      var actual = controller.executeSearch(request);

      // then
      var expected =
          ResponseEntity.badRequest()
              .body(new ApiError("STBLPAY-6001", "Index not in allowlist: secret", FIXED_NOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn503OnFailed() {
      // given
      var request = validBoolTermRequest();
      var failed = new SearchExecutionResult.Failed("STBLPAY-6006", "Search execution failed");
      given(searchService.execute(request)).willReturn(failed);

      // when
      var actual = controller.executeSearch(request);

      // then
      var expected =
          ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body(new ApiError("STBLPAY-6006", "Search execution failed", FIXED_NOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class FetchTimeline {

    @Test
    void shouldReturn200WithTimelineOnFound() {
      // given
      var timeline = new Timeline(SOME_REFERENCE, "# Timeline", SOME_TIMELINE_ENTRIES);
      given(timelineService.fetch(SOME_REFERENCE)).willReturn(new FetchResult.Found(timeline));

      // when
      var actual = controller.fetchTimeline(SOME_REFERENCE);

      // then
      var expectedEntries =
          SOME_TIMELINE_ENTRIES.stream()
              .map(
                  e ->
                      TimelineEntryDto.builder()
                          .eventId(e.eventId())
                          .source(e.source())
                          .status(e.status())
                          .detail(e.detail())
                          .timestamp(e.timestamp())
                          .build())
              .toList();
      var expected =
          ResponseEntity.ok(
              AgentTimelineResponse.builder()
                  .reference(SOME_REFERENCE)
                  .markdown("# Timeline")
                  .entries(expectedEntries)
                  .build());
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn404OnNotFound() {
      // given
      given(timelineService.fetch(SOME_REFERENCE)).willReturn(new FetchResult.NotFound());

      // when
      var actual = controller.fetchTimeline(SOME_REFERENCE);

      // then
      var expected =
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(new ApiError("STBLPAY-3001", "Timeline not found", FIXED_NOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
