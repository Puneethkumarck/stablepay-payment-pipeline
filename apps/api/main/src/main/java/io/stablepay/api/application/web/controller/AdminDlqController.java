package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.annotation.Idempotent;
import io.stablepay.api.application.web.dto.DlqEventDto;
import io.stablepay.api.application.web.dto.DlqReplayResponse;
import io.stablepay.api.application.web.dto.DlqSummaryDto;
import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.mapper.DlqEventWebMapper;
import io.stablepay.api.application.web.mapper.DlqSummaryWebMapper;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.service.DlqReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dlq")
@Secured("ROLE_ADMIN")
@Tag(name = "admin")
public class AdminDlqController {

  private final DlqRepository dlqRepository;
  private final DlqReplayService dlqReplayService;
  private final DlqEventWebMapper mapper;
  private final DlqSummaryWebMapper dlqSummaryMapper;
  private final Clock clock;

  @Operation(summary = "List dead-letter queue events")
  @ApiResponse(responseCode = "200", description = "Paginated DLQ event list")
  @GetMapping
  public PaginatedResponse<DlqEventDto> list(
      @Parameter(description = "Opaque pagination cursor") @RequestParam Optional<String> cursor,
      @Parameter(description = "Page size (1–100)")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var result = dlqRepository.searchAdmin(size, cursor);
    return mapper.toResponse(result);
  }

  @Operation(summary = "Get DLQ summary counts by error class")
  @ApiResponse(responseCode = "200", description = "DLQ summary returned")
  @GetMapping("/summary")
  public DlqSummaryDto summary(@AuthenticationPrincipal AuthenticatedUser user) {
    var summary = dlqRepository.summaryAdmin();
    return dlqSummaryMapper.toDto(summary);
  }

  @Operation(summary = "Find DLQ event by ID")
  @ApiResponse(responseCode = "200", description = "DLQ event found")
  @ApiResponse(
      responseCode = "404",
      description = "DLQ event not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @GetMapping("/{id}")
  public ResponseEntity<DlqEventDto> findById(
      @Parameter(description = "DLQ event UUID") @PathVariable String id,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return dlqRepository
        .findByIdAdmin(DlqId.of(UUID.fromString(id)))
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("DLQ event", id));
  }

  @Operation(summary = "Replay a DLQ event")
  @ApiResponse(responseCode = "200", description = "Replay accepted")
  @ApiResponse(
      responseCode = "404",
      description = "DLQ event not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Idempotency key conflict",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @PostMapping("/{id}/replay")
  @Idempotent
  public ResponseEntity<DlqReplayResponse> replay(
      @Parameter(description = "DLQ event UUID") @PathVariable String id,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var dlqId = DlqId.of(UUID.fromString(id));
    dlqReplayService.replay(dlqId, user.userId());
    return ResponseEntity.ok(
        DlqReplayResponse.builder()
            .dlqId(id)
            .status("ACCEPTED")
            .timestamp(clock.instant())
            .build());
  }
}
