package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.web.dto.AgentSearchResponse;
import io.stablepay.api.application.web.dto.AgentSqlRequest;
import io.stablepay.api.application.web.dto.AgentSqlResponse;
import io.stablepay.api.application.web.dto.AgentTimelineResponse;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.agent.AgentSearchRequest;
import io.stablepay.api.domain.agent.AgentSearchService;
import io.stablepay.api.domain.agent.AgentSqlService;
import io.stablepay.api.domain.agent.AgentTimelineService;
import io.stablepay.api.domain.agent.FetchResult;
import io.stablepay.api.domain.agent.SearchExecutionResult;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import jakarta.validation.Valid;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
@Secured("ROLE_AGENT")
public class AgentController {

  private static final String NOT_FOUND_CODE = "STBLPAY-3001";

  private final AgentSqlService sqlService;
  private final AgentSearchService searchService;
  private final AgentTimelineService timelineService;
  private final Clock clock;

  @PostMapping("/sql")
  public ResponseEntity<?> executeSql(@Valid @RequestBody AgentSqlRequest request) {
    var limit = request.limit().orElse(1_000);
    var result = sqlService.execute(request.sql(), limit);
    return switch (result) {
      case SqlExecutionResult.Success success ->
          ResponseEntity.ok(
              AgentSqlResponse.builder()
                  .columns(success.columns())
                  .rows(success.rows())
                  .rowCount(success.rowCount())
                  .queryId(success.queryId())
                  .tookMs(success.tookMs())
                  .build());
      case SqlExecutionResult.Rejected rejected ->
          ResponseEntity.badRequest()
              .body(new ApiError(rejected.errorCode(), rejected.reason(), clock.instant()));
      case SqlExecutionResult.Failed failed ->
          ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body(new ApiError(failed.errorCode(), failed.reason(), clock.instant()));
    };
  }

  @PostMapping("/search")
  public ResponseEntity<?> executeSearch(@Valid @RequestBody AgentSearchRequest request) {
    var result = searchService.execute(request);
    return switch (result) {
      case SearchExecutionResult.Success success ->
          ResponseEntity.ok(
              AgentSearchResponse.builder()
                  .hits(success.hits())
                  .totalHits(success.totalHits())
                  .resolvedSize(success.resolvedSize())
                  .tookMs(success.tookMs())
                  .build());
      case SearchExecutionResult.Rejected rejected ->
          ResponseEntity.badRequest()
              .body(new ApiError(rejected.errorCode(), rejected.reason(), clock.instant()));
      case SearchExecutionResult.Failed failed ->
          ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body(new ApiError(failed.errorCode(), failed.reason(), clock.instant()));
    };
  }

  @GetMapping("/timeline/{ref}")
  public ResponseEntity<?> fetchTimeline(@PathVariable String ref) {
    var result = timelineService.fetch(ref);
    return switch (result) {
      case FetchResult.Found found -> {
        var timeline = found.timeline();
        var entries =
            timeline.entries().stream()
                .map(
                    e ->
                        AgentTimelineResponse.TimelineEntryDto.builder()
                            .eventId(e.eventId())
                            .source(e.source())
                            .status(e.status())
                            .detail(e.detail())
                            .timestamp(e.timestamp())
                            .build())
                .toList();
        yield ResponseEntity.ok(
            AgentTimelineResponse.builder()
                .reference(timeline.reference())
                .markdown(timeline.markdown())
                .entries(entries)
                .build());
      }
      case FetchResult.NotFound notFound ->
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(new ApiError(NOT_FOUND_CODE, "Timeline not found", clock.instant()));
    };
  }
}
