package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.DashboardStatsDto;
import io.stablepay.api.application.web.mapper.DashboardStatsWebMapper;
import io.stablepay.api.domain.port.DashboardStatsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Secured("ROLE_CUSTOMER")
@Tag(name = "customer")
public class DashboardController {

  private final DashboardStatsRepository dashboardStatsRepository;
  private final DashboardStatsWebMapper mapper;

  @Operation(summary = "Get dashboard statistics for the authenticated customer")
  @ApiResponse(responseCode = "200", description = "Dashboard stats returned")
  @GetMapping("/stats")
  public DashboardStatsDto stats(@AuthenticationPrincipal AuthenticatedUser user) {
    var stats = dashboardStatsRepository.getStats(user.requireCustomerId());
    return mapper.toDto(stats);
  }
}
