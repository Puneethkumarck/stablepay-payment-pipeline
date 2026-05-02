package io.stablepay.api.application.web.controller;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.TransactionEventDto;
import io.stablepay.api.application.web.mapper.TransactionEventWebMapper;
import io.stablepay.api.domain.model.TransactionEvent;
import io.stablepay.api.domain.port.TransactionEventSource;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/streams")
@Secured({"ROLE_CUSTOMER", "ROLE_ADMIN"})
public class StreamsController {

  private final TransactionEventSource eventSource;
  private final TransactionEventWebMapper mapper;

  @GetMapping(value = "/transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<TransactionEventDto>> transactions(
      @AuthenticationPrincipal AuthenticatedUser user) {
    var events =
        eventSource
            .subscribeAdmin()
            .filter(e -> isVisibleTo(e, user))
            .map(
                e ->
                    ServerSentEvent.<TransactionEventDto>builder()
                        .id(e.eventId())
                        .data(mapper.toDto(e))
                        .retry(Duration.ofSeconds(2))
                        .build());
    var heartbeat =
        Flux.interval(Duration.ofSeconds(30))
            .map(__ -> ServerSentEvent.<TransactionEventDto>builder().comment("heartbeat").build());
    return events.mergeWith(heartbeat);
  }

  @GetMapping("/transactions/recent")
  public List<TransactionEventDto> recent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) String since) {
    return eventSource.snapshotSinceAdmin(Optional.ofNullable(since), 50).stream()
        .filter(e -> isVisibleTo(e, user))
        .map(mapper::toDto)
        .toList();
  }

  private static boolean isVisibleTo(TransactionEvent event, AuthenticatedUser user) {
    return user.isAdmin()
        || user.customerId().map(c -> c.value().toString()).orElse("").equals(event.customerId());
  }
}
