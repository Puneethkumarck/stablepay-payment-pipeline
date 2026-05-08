package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.FlowDto;
import io.stablepay.api.application.web.mapper.FlowWebMapper;
import io.stablepay.api.client.ApiError;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.port.FlowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/flows")
@Secured("ROLE_CUSTOMER")
@Tag(name = "customer")
public class FlowController {

  private final FlowRepository flowRepository;
  private final FlowWebMapper mapper;

  @Operation(summary = "Find payment flow by ID")
  @ApiResponse(responseCode = "200", description = "Flow found")
  @ApiResponse(
      responseCode = "404",
      description = "Flow not found",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @GetMapping("/{id}")
  public ResponseEntity<FlowDto> findById(
      @Parameter(description = "Flow UUID") @PathVariable String id,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return flowRepository
        .findById(FlowId.of(UUID.fromString(id)), user.requireCustomerId())
        .map(mapper::toDto)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new NotFoundException("Flow", id));
  }
}
