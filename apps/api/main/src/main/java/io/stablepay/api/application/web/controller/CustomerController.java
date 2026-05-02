package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.CustomerSummaryDto;
import io.stablepay.api.application.web.mapper.CustomerSummaryWebMapper;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.port.CustomerRepository;
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
@RequestMapping("/api/v1/customers")
@Secured("ROLE_CUSTOMER")
public class CustomerController {

  private final CustomerRepository customerRepository;
  private final CustomerSummaryWebMapper mapper;

  @GetMapping("/{id}/summary")
  public ResponseEntity<CustomerSummaryDto> summary(
      @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
    var requestedId = UUID.fromString(id);
    if (!requestedId.equals(user.requireCustomerId().value())) {
      throw new NotFoundException("Customer", id);
    }
    return customerRepository
        .findById(user.requireCustomerId())
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("Customer", id));
  }
}
