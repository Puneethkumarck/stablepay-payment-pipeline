package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.CustomerSummaryDto;
import io.stablepay.api.application.web.mapper.CustomerSummaryWebMapper;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.port.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/customers")
@Secured("ROLE_ADMIN")
@Tag(name = "admin")
public class AdminCustomerController {

  private final CustomerRepository customerRepository;
  private final CustomerSummaryWebMapper mapper;

  @Operation(summary = "Get customer summary (admin)")
  @ApiResponse(responseCode = "200", description = "Customer summary returned")
  @ApiResponse(
      responseCode = "404",
      description = "Customer not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @GetMapping("/{id}/summary")
  public ResponseEntity<CustomerSummaryDto> summary(
      @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
    return customerRepository
        .findByIdAdmin(CustomerId.of(UUID.fromString(id)))
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("Customer", id));
  }
}
