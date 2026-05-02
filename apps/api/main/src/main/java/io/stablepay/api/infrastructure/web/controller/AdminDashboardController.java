package io.stablepay.api.infrastructure.web.controller;

import io.stablepay.api.domain.port.DashboardStatsRepository;
import io.stablepay.api.infrastructure.security.AuthenticatedUser;
import io.stablepay.api.infrastructure.web.dto.DashboardStatsDto;
import io.stablepay.api.infrastructure.web.mapper.DashboardStatsWebMapper;
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
@RequestMapping("/api/v1/admin/dashboard")
@Secured("ROLE_ADMIN")
public class AdminDashboardController {

  private final DashboardStatsRepository dashboardStatsRepository;
  private final DashboardStatsWebMapper mapper;

  @GetMapping("/stats")
  public DashboardStatsDto stats(@AuthenticationPrincipal AuthenticatedUser user) {
    var stats = dashboardStatsRepository.getStatsAdmin();
    return mapper.toDto(stats);
  }
}
