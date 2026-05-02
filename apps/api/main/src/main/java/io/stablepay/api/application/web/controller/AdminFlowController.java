package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.FlowDto;
import io.stablepay.api.application.web.mapper.FlowWebMapper;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.port.FlowRepository;
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
@RequestMapping("/api/v1/admin/flows")
@Secured("ROLE_ADMIN")
public class AdminFlowController {

  private final FlowRepository flowRepository;
  private final FlowWebMapper mapper;

  @GetMapping("/{id}")
  public ResponseEntity<FlowDto> findById(
      @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser user) {
    return flowRepository
        .findByIdAdmin(FlowId.of(UUID.fromString(id)))
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("Flow", id));
  }
}
