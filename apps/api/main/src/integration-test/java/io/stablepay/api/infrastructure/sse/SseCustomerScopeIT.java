package io.stablepay.api.infrastructure.sse;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.web.controller.StreamsController;
import io.stablepay.api.application.web.dto.TransactionEventDto;
import io.stablepay.api.application.web.mapper.TransactionEventWebMapper;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.port.TransactionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
class SseCustomerScopeIT {

  private static final CustomerId BOB_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));

  @Mock private TransactionRepository transactionRepository;

  private StreamsController controller;
  private SseTransactionPoller poller;

  @BeforeEach
  void setUp() {
    var eventMapper = Mappers.getMapper(TransactionEventMapper.class);
    var webMapper = Mappers.getMapper(TransactionEventWebMapper.class);
    poller = new SseTransactionPoller(transactionRepository, eventMapper);
    controller = new StreamsController(poller, webMapper);
  }

  @Nested
  class SseStream {

    @Test
    void shouldNotDeliverBobsEventToAlice() {
      // given
      var alice = someCustomerUser();
      var bobTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(BOB_CUSTOMER_ID)
              .eventId("evt-bob-001")
              .build();
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willReturn(Stream.of(bobTx));

      // when
      var flux = controller.transactions(alice);
      poller.poll();

      // then
      StepVerifier.create(flux.take(1))
          .expectNextCount(0)
          .thenCancel()
          .verify(java.time.Duration.ofMillis(200));
    }

    @Test
    void shouldDeliverBothCustomerEventsToAdmin() {
      // given
      var admin = someAdminUser();
      var aliceTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(SOME_CUSTOMER_ID)
              .eventId("evt-alice-001")
              .build();
      var bobTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(BOB_CUSTOMER_ID)
              .eventId("evt-bob-001")
              .build();
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 100))
          .willReturn(Stream.of(aliceTx, bobTx));

      // when
      var flux = controller.transactions(admin);
      poller.poll();

      // then
      StepVerifier.create(flux.take(2))
          .assertNext(
              sse -> assertThat(sse.data()).extracting(TransactionEventDto::customerId)
                  .isEqualTo(SOME_CUSTOMER_ID.value().toString()))
          .assertNext(
              sse -> assertThat(sse.data()).extracting(TransactionEventDto::customerId)
                  .isEqualTo(BOB_CUSTOMER_ID.value().toString()))
          .verifyComplete();
    }
  }

  @Nested
  class PollingFallback {

    @Test
    void shouldNotReturnBobsEventToAlice() {
      // given
      var alice = someCustomerUser();
      var aliceTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(SOME_CUSTOMER_ID)
              .eventId("evt-alice-001")
              .build();
      var bobTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(BOB_CUSTOMER_ID)
              .eventId("evt-bob-001")
              .build();
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 50))
          .willReturn(Stream.of(aliceTx, bobTx));

      // when
      var result = controller.recent(alice, null);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().customerId())
          .isEqualTo(SOME_CUSTOMER_ID.value().toString());
    }

    @Test
    void shouldReturnBothCustomerEventsToAdmin() {
      // given
      var admin = someAdminUser();
      var aliceTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(SOME_CUSTOMER_ID)
              .eventId("evt-alice-001")
              .build();
      var bobTx =
          SOME_TRANSACTION.toBuilder()
              .customerId(BOB_CUSTOMER_ID)
              .eventId("evt-bob-001")
              .build();
      given(transactionRepository.tailSinceSortValueAdmin(Optional.empty(), 50))
          .willReturn(Stream.of(aliceTx, bobTx));

      // when
      var result = controller.recent(admin, null);

      // then
      assertThat(result).hasSize(2);
    }
  }
}
