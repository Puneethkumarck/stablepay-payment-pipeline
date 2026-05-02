package io.stablepay.api.infrastructure.web.controller;

import io.stablepay.api.domain.port.StuckRepository;
import io.stablepay.api.infrastructure.security.AuthenticatedUser;
import io.stablepay.api.infrastructure.web.dto.PaginatedResponse;
import io.stablepay.api.infrastructure.web.dto.StuckPaymentDto;
import io.stablepay.api.infrastructure.web.mapper.StuckPaymentWebMapper;
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
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var result = stuckRepository.searchAdmin(size, cursor);
    return mapper.toResponse(result);
  }
}
