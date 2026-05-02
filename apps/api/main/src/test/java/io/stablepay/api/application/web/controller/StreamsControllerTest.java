package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.domain.model.fixtures.TransactionEventFixtures.SOME_OTHER_CUSTOMER_EVENT;
import static io.stablepay.api.domain.model.fixtures.TransactionEventFixtures.SOME_TRANSACTION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.web.dto.TransactionEventDto;
import io.stablepay.api.application.web.mapper.TransactionEventWebMapper;
import io.stablepay.api.domain.model.TransactionEvent;
import io.stablepay.api.domain.port.TransactionEventSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StreamsControllerTest {

  @Mock private TransactionEventSource eventSource;

  @Spy
  private final TransactionEventWebMapper mapper =
      Mappers.getMapper(TransactionEventWebMapper.class);

  @InjectMocks private StreamsController controller;

  @Nested
  class SseStream {

    @Test
    void shouldFilterEventsToCustomerScope() {
      // given
      var user = someCustomerUser();
      var ownEvent =
          SOME_TRANSACTION_EVENT.toBuilder()
              .customerId(SOME_CUSTOMER_ID.value().toString())
              .build();
      var otherEvent = SOME_OTHER_CUSTOMER_EVENT;
      given(eventSource.subscribeAdmin()).willReturn(Flux.just(ownEvent, otherEvent));

      // when
      var result = controller.transactions(user);

      // then
      StepVerifier.create(result.take(1))
          .assertNext(
              sse -> {
                assertThat(sse.id()).isEqualTo(ownEvent.eventId());
                assertThat(sse.data())
                    .usingRecursiveComparison()
                    .isEqualTo(toExpectedDto(ownEvent));
              })
          .verifyComplete();
    }

    @Test
    void shouldPassAllEventsToAdmin() {
      // given
      var user = someAdminUser();
      var event1 =
          SOME_TRANSACTION_EVENT.toBuilder()
              .customerId(SOME_CUSTOMER_ID.value().toString())
              .build();
      var event2 = SOME_OTHER_CUSTOMER_EVENT;
      given(eventSource.subscribeAdmin()).willReturn(Flux.just(event1, event2));

      // when
      var result = controller.transactions(user);

      // then
      StepVerifier.create(result.take(2))
          .assertNext(
              sse ->
                  assertThat(sse.data())
                      .usingRecursiveComparison()
                      .isEqualTo(toExpectedDto(event1)))
          .assertNext(
              sse ->
                  assertThat(sse.data())
                      .usingRecursiveComparison()
                      .isEqualTo(toExpectedDto(event2)))
          .verifyComplete();
    }

    @Test
    void shouldIncludeRetryDirective() {
      // given
      var user = someAdminUser();
      given(eventSource.subscribeAdmin()).willReturn(Flux.just(SOME_TRANSACTION_EVENT));

      // when
      var result = controller.transactions(user);

      // then
      StepVerifier.create(result.take(1))
          .assertNext(sse -> assertThat(sse.retry()).isEqualTo(java.time.Duration.ofSeconds(2)))
          .verifyComplete();
    }
  }

  @Nested
  class PollingFallback {

    @Test
    void shouldFilterRecentEventsToCustomerScope() {
      // given
      var user = someCustomerUser();
      var ownEvent =
          SOME_TRANSACTION_EVENT.toBuilder()
              .customerId(SOME_CUSTOMER_ID.value().toString())
              .build();
      var otherEvent = SOME_OTHER_CUSTOMER_EVENT;
      given(eventSource.snapshotSinceAdmin(Optional.empty(), 50))
          .willReturn(List.of(ownEvent, otherEvent));

      // when
      var result = controller.recent(user, null);

      // then
      assertThat(result).usingRecursiveComparison().isEqualTo(List.of(toExpectedDto(ownEvent)));
    }

    @Test
    void shouldReturnAllRecentEventsForAdmin() {
      // given
      var user = someAdminUser();
      var event1 =
          SOME_TRANSACTION_EVENT.toBuilder()
              .customerId(SOME_CUSTOMER_ID.value().toString())
              .build();
      var event2 = SOME_OTHER_CUSTOMER_EVENT;
      given(eventSource.snapshotSinceAdmin(Optional.empty(), 50))
          .willReturn(List.of(event1, event2));

      // when
      var result = controller.recent(user, null);

      // then
      assertThat(result)
          .usingRecursiveComparison()
          .isEqualTo(List.of(toExpectedDto(event1), toExpectedDto(event2)));
    }

    @Test
    void shouldPassSinceParameterToSource() {
      // given
      var user = someAdminUser();
      var cursor = "some-cursor";
      given(eventSource.snapshotSinceAdmin(Optional.of(cursor), 50)).willReturn(List.of());

      // when
      var result = controller.recent(user, cursor);

      // then
      assertThat(result).isEmpty();
    }
  }

  private static TransactionEventDto toExpectedDto(TransactionEvent event) {
    return TransactionEventDto.builder()
        .eventId(event.eventId())
        .customerId(event.customerId())
        .status(event.status())
        .eventTime(event.eventTime())
        .amountMicros(event.amountMicros())
        .currencyCode(event.currencyCode())
        .flowType(event.flowType())
        .sortKey(event.sortKey())
        .build();
  }
}
