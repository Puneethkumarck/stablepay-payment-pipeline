package io.stablepay.api.infrastructure.web.controller;

import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.port.TransactionRepository;
import io.stablepay.api.infrastructure.security.AuthenticatedUser;
import io.stablepay.api.infrastructure.web.dto.TransactionDto;
import io.stablepay.api.infrastructure.web.mapper.TransactionWebMapper;
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
public class AdminTransactionController {

  private final TransactionRepository transactionRepository;
  private final TransactionWebMapper mapper;

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
