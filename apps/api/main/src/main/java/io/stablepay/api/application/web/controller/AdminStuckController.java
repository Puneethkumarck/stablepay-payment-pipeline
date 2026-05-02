package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.dto.StuckPaymentDto;
import io.stablepay.api.application.web.mapper.StuckPaymentWebMapper;
import io.stablepay.api.domain.port.StuckRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stuck")
@Secured("ROLE_ADMIN")
public class AdminStuckController {

  private final StuckRepository stuckRepository;
  private final StuckPaymentWebMapper mapper;

  @GetMapping
  public PaginatedResponse<StuckPaymentDto> list(
      @RequestParam Optional<String> cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var result = stuckRepository.searchAdmin(size, cursor);
    return mapper.toResponse(result);
  }
}
