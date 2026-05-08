package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.web.dto.AgentSearchResponse;
import io.stablepay.api.application.web.dto.AgentSqlRequest;
import io.stablepay.api.application.web.dto.AgentSqlResponse;
import io.stablepay.api.application.web.dto.AgentTimelineResponse;
import io.stablepay.api.application.web.dto.TimelineEntryDto;
import io.stablepay.api.application.web.error.ErrorCodes;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.agent.AgentSearchRequest;
import io.stablepay.api.domain.agent.AgentSearchService;
import io.stablepay.api.domain.agent.AgentSqlService;
import io.stablepay.api.domain.agent.AgentTimelineService;
import io.stablepay.api.domain.agent.FetchResult;
import io.stablepay.api.domain.agent.SearchExecutionResult;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "agent")
public class AgentController {

  private final AgentSqlService sqlService;
  private final AgentSearchService searchService;
  private final AgentTimelineService timelineService;
  private final Clock clock;

  @Operation(summary = "Execute a validated Trino SQL query")
  @ApiResponse(
      responseCode = "200",
      description = "Query executed successfully",
      content = @Content(schema = @Schema(implementation = AgentSqlResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "SQL rejected by allowlist validator",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "503",
      description = "Trino query execution failed",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
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

  @Operation(summary = "Execute a validated OpenSearch DSL query")
  @ApiResponse(
      responseCode = "200",
      description = "Search executed successfully",
      content = @Content(schema = @Schema(implementation = AgentSearchResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "DSL rejected by whitelist validator",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "503",
      description = "OpenSearch query execution failed",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
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

  @Operation(summary = "Fetch transaction timeline by reference")
  @ApiResponse(
      responseCode = "200",
      description = "Timeline found",
      content = @Content(schema = @Schema(implementation = AgentTimelineResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Timeline not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
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
                        TimelineEntryDto.builder()
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
              .body(new ApiError(ErrorCodes.NOT_FOUND, "Timeline not found", clock.instant()));
    };
  }
}
