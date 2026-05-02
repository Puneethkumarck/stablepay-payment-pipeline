package io.stablepay.api.infrastructure.sse;

import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionEvent;
import io.stablepay.api.domain.port.TransactionRepository;
import io.stablepay.api.infrastructure.cursor.Base64PipeCursor;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SseTransactionPollerTest {

  @Mock private TransactionRepository transactionRepository;

  @Spy
  private final TransactionEventMapper mapper = Mappers.getMapper(TransactionEventMapper.class);

  @InjectMocks private SseTransactionPoller poller;

  @Nested
  class Poll {

    @Test
    void shouldEmitMappedEventsToSubscribers() {
      // given
      var tx = SOME_TRANSACTION;
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willReturn(Stream.of(tx));
      var expectedSortKey = Base64PipeCursor.encode(tx.eventTime().toEpochMilli(), tx.eventId());
      var expected = buildExpectedEvent(tx, expectedSortKey);

      // when
      var flux = poller.subscribeAdmin();
      poller.poll();

      // then
      StepVerifier.create(flux.take(1))
          .assertNext(event -> assertThat(event).usingRecursiveComparison().isEqualTo(expected))
          .verifyComplete();
    }

    @Test
    void shouldNotEmitWhenNoNewTransactions() {
      // given
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willReturn(Stream.empty());

      // when
      poller.poll();

      // then
      StepVerifier.create(poller.subscribeAdmin().take(1))
          .expectNextCount(0)
          .thenCancel()
          .verify(Duration.ofMillis(100));
    }

    @Test
    void shouldUpdateLastSortValueAfterPoll() {
      // given
      var tx = SOME_TRANSACTION;
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willReturn(Stream.of(tx));
      var expectedCursor = Base64PipeCursor.encode(tx.eventTime().toEpochMilli(), tx.eventId());

      // when
      poller.poll();

      // then — second poll uses updated cursor
      given(transactionRepository.tailSinceSortValueAdmin(Optional.of(expectedCursor), 100))
          .willReturn(Stream.empty());
      poller.poll();
    }

    @Test
    void shouldNotCrashOnException() {
      // given
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willThrow(new RuntimeException("OpenSearch down"));

      // when — poll catches the exception internally
      poller.poll();

      // then — poller is still functional after exception
    }
  }

  @Nested
  class SnapshotSince {

    @Test
    void shouldReturnMappedEventsFromRepository() {
      // given
      var tx = SOME_TRANSACTION;
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 50))
          .willReturn(Stream.of(tx));
      var expectedSortKey = Base64PipeCursor.encode(tx.eventTime().toEpochMilli(), tx.eventId());
      var expected = buildExpectedEvent(tx, expectedSortKey);

      // when
      var result = poller.snapshotSinceAdmin(Optional.empty(), 50);

      // then
      assertThat(result).usingRecursiveComparison().isEqualTo(List.of(expected));
    }

    @Test
    void shouldPassCursorToRepository() {
      // given
      var cursor = "some-cursor-value";
      given(transactionRepository.tailSinceSortValueAdmin(Optional.of(cursor), 50))
          .willReturn(Stream.empty());

      // when
      var result = poller.snapshotSinceAdmin(Optional.of(cursor), 50);

      // then
      assertThat(result).isEmpty();
    }
  }

  private static TransactionEvent buildExpectedEvent(Transaction tx, String sortKey) {
    return TransactionEvent.builder()
        .eventId(tx.eventId())
        .customerId(tx.customerId().value().toString())
        .status(tx.customerStatus())
        .eventTime(tx.eventTime())
        .amountMicros(tx.amount().toMicros())
        .currencyCode(tx.amount().currency().name())
        .flowType(tx.flowType())
        .sortKey(sortKey)
        .build();
  }
}
