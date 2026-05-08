package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.TransactionDto;
import io.stablepay.api.application.web.mapper.TransactionWebMapper;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.port.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/admin/transactions")
@Secured("ROLE_ADMIN")
@Tag(name = "admin")
public class AdminTransactionController {

  private final TransactionRepository transactionRepository;
  private final TransactionWebMapper mapper;

  @Operation(summary = "Find transaction by reference (admin)")
  @ApiResponse(responseCode = "200", description = "Transaction found")
  @ApiResponse(
      responseCode = "404",
      description = "Transaction not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @GetMapping("/{ref}")
  public ResponseEntity<TransactionDto> findByReference(
      @PathVariable String ref, @AuthenticationPrincipal AuthenticatedUser user) {
    return transactionRepository
        .findByReferenceAdmin(ref)
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("Transaction", ref));
  }
}
