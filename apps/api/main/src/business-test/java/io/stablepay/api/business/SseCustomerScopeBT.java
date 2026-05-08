package io.stablepay.api.business;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_UUID;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.TransactionEventDto;
import io.stablepay.api.config.BusinessTestBase;
import io.stablepay.api.domain.model.TransactionEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

class SseCustomerScopeBT extends BusinessTestBase {

  private static final String ALICE_CUSTOMER_ID = SOME_CUSTOMER_UUID.toString();
  private static final String BOB_CUSTOMER_ID_STR =
      UUID.fromString("33333333-3333-3333-3333-333333333333").toString();

  @BeforeEach
  void clearEventBuffer() {
    stubRepositoryConfig.clearEvents();
  }

  @Test
  void aliceShouldOnlySeeHerOwnEvents() {
    // given
    var aliceEvent =
        TransactionEvent.builder()
            .eventId("evt-alice-bt-001")
            .customerId(ALICE_CUSTOMER_ID)
            .status("COMPLETED")
            .eventTime(Instant.parse("2026-05-01T10:00:00Z"))
            .amountMicros(100_000_000L)
            .currencyCode("USD")
            .flowType("CRYPTO_PAYIN")
            .sortKey("YWxpY2Uta2V5")
            .build();
    var bobEvent =
        TransactionEvent.builder()
            .eventId("evt-bob-bt-001")
            .customerId(BOB_CUSTOMER_ID_STR)
            .status("COMPLETED")
            .eventTime(Instant.parse("2026-05-01T10:00:01Z"))
            .amountMicros(200_000_000L)
            .currencyCode("USD")
            .flowType("FIAT_PAYIN")
            .sortKey("Ym9iLWtleQ")
            .build();
    stubRepositoryConfig.emitEvent(aliceEvent);
    stubRepositoryConfig.emitEvent(bobEvent);
    var client = authenticatedClient(aliceJwt());

    // when
    var result =
        client
            .get()
            .uri("/api/v1/streams/transactions/recent")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<java.util.List<TransactionEventDto>>() {});

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().customerId()).isEqualTo(ALICE_CUSTOMER_ID);
  }

  @Test
  void adminShouldSeeAllEvents() {
    // given
    var aliceEvent =
        TransactionEvent.builder()
            .eventId("evt-alice-bt-002")
            .customerId(ALICE_CUSTOMER_ID)
            .status("COMPLETED")
            .eventTime(Instant.parse("2026-05-01T10:00:00Z"))
            .amountMicros(100_000_000L)
            .currencyCode("USD")
            .flowType("CRYPTO_PAYIN")
            .sortKey("YWxpY2Uta2V5Mg")
            .build();
    var bobEvent =
        TransactionEvent.builder()
            .eventId("evt-bob-bt-002")
            .customerId(BOB_CUSTOMER_ID_STR)
            .status("COMPLETED")
            .eventTime(Instant.parse("2026-05-01T10:00:01Z"))
            .amountMicros(200_000_000L)
            .currencyCode("USD")
            .flowType("FIAT_PAYIN")
            .sortKey("Ym9iLWtleTI")
            .build();
    stubRepositoryConfig.emitEvent(aliceEvent);
    stubRepositoryConfig.emitEvent(bobEvent);
    var client = authenticatedClient(adminJwt());

    // when
    var result =
        client
            .get()
            .uri("/api/v1/streams/transactions/recent")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<java.util.List<TransactionEventDto>>() {});

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
  }
}
