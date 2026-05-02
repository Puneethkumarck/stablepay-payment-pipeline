package io.stablepay.api.infrastructure.sse;

import io.stablepay.api.domain.model.TransactionEvent;
import io.stablepay.api.domain.port.TransactionEventSource;
import io.stablepay.api.domain.port.TransactionRepository;
import io.stablepay.api.infrastructure.cursor.Base64PipeCursor;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseTransactionPoller implements TransactionEventSource {

  private final TransactionRepository transactionRepository;
  private final TransactionEventMapper mapper;

  private final Sinks.Many<TransactionEvent> sink =
      Sinks.many().multicast().onBackpressureBuffer(256);

  private final AtomicReference<Optional<String>> lastSortValue =
      new AtomicReference<>(Optional.empty());

  @Scheduled(fixedDelay = 1000)
  void poll() {
    try (var stream = transactionRepository.tailSinceSortValueAdmin(lastSortValue.get(), 100)) {
      stream.forEach(
          tx -> {
            var sortKey = Base64PipeCursor.encode(tx.eventTime().toEpochMilli(), tx.eventId());
            var event = mapper.toEvent(tx).toBuilder().sortKey(sortKey).build();
            sink.tryEmitNext(event);
            lastSortValue.set(Optional.of(sortKey));
          });
    } catch (Exception e) {
      log.warn("SSE poll failed: {}", e.getMessage(), e);
    }
  }

  @Override
  public Flux<TransactionEvent> subscribeAdmin() {
    return sink.asFlux();
  }

  @Override
  public List<TransactionEvent> snapshotSinceAdmin(Optional<String> since, int limit) {
    try (var stream = transactionRepository.tailSinceSortValueAdmin(since, limit)) {
      return stream
          .map(
              tx -> {
                var sortKey = Base64PipeCursor.encode(tx.eventTime().toEpochMilli(), tx.eventId());
                return mapper.toEvent(tx).toBuilder().sortKey(sortKey).build();
              })
          .toList();
    }
  }
}
