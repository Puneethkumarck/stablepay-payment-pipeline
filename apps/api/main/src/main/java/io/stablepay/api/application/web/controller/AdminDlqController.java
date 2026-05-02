package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.annotation.Idempotent;
import io.stablepay.api.application.web.dto.DlqEventDto;
import io.stablepay.api.application.web.dto.DlqReplayResponse;
import io.stablepay.api.application.web.dto.DlqSummaryDto;
import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.mapper.DlqEventWebMapper;
import io.stablepay.api.application.web.mapper.DlqSummaryWebMapper;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.service.DlqReplayService;
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
public class AdminDlqController {

  private final DlqRepository dlqRepository;
  private final DlqReplayService dlqReplayService;
  private final DlqEventWebMapper mapper;
  private final DlqSummaryWebMapper dlqSummaryMapper;
  private final Clock clock;

  @GetMapping
  public PaginatedResponse<DlqEventDto> list(
      @RequestParam Optional<String> cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var result = dlqRepository.searchAdmin(size, cursor);
    return mapper.toResponse(result);
  }

  @GetMapping("/summary")
  public DlqSummaryDto summary(@AuthenticationPrincipal AuthenticatedUser user) {
    var summary = dlqRepository.summaryAdmin();
    return dlqSummaryMapper.toDto(summary);
  }

  @GetMapping("/{id}")
  public ResponseEntity<DlqEventDto> findById(
      @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
    return dlqRepository
        .findByIdAdmin(DlqId.of(UUID.fromString(id)))
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("DLQ event", id));
  }

  @PostMapping("/{id}/replay")
  @Idempotent
  public DlqReplayResponse replay(
      @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
    var dlqId = DlqId.of(UUID.fromString(id));
    dlqReplayService.replay(dlqId, user.userId());
    return DlqReplayResponse.builder()
        .dlqId(id)
        .status("ACCEPTED")
        .timestamp(clock.instant())
        .build();
  }
}
