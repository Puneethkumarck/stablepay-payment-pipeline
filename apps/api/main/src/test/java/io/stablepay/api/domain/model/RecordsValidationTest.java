package io.stablepay.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.stablepay.api.domain.model.fixtures.CustomerSummaryFixtures;
import io.stablepay.api.domain.model.fixtures.DlqEventFixtures;
import io.stablepay.api.domain.model.fixtures.FlowFixtures;
import io.stablepay.api.domain.model.fixtures.MoneyFixtures;
import io.stablepay.api.domain.model.fixtures.StuckPaymentFixtures;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordsValidationTest {

  // ---------- Transaction ----------

  @Test
  void transaction_builder_buildsExpectedTransaction() {
    // given
    var expected =
        new Transaction(
            TransactionFixtures.SOME_TRANSACTION_ID,
            TransactionFixtures.SOME_REFERENCE,
            "CRYPTO_PAYIN",
            "CONFIRMED",
            "COMPLETED",
            MoneyFixtures.SOME_MONEY,
            TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID,
            TransactionFixtures.SOME_TRANSACTION_ACCOUNT_ID,
            Optional.of("0xabcdef"),
            TransactionFixtures.SOME_TRANSACTION_FLOW_ID,
            "evt-0001",
            "corr-0001",
            "trace-0001",
            TransactionFixtures.SOME_EVENT_TIME,
            TransactionFixtures.SOME_INGEST_TIME,
            Map.of("chain", "ethereum", "asset", "USDC"));

    // when
    var actual = TransactionFixtures.SOME_TRANSACTION;

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void transaction_nullId_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                Transaction.builder()
                    .id(null)
                    .reference(TransactionFixtures.SOME_REFERENCE)
                    .flowType("CRYPTO_PAYIN")
                    .internalStatus("CONFIRMED")
                    .customerStatus("COMPLETED")
                    .amount(MoneyFixtures.SOME_MONEY)
                    .customerId(TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID)
                    .accountId(TransactionFixtures.SOME_TRANSACTION_ACCOUNT_ID)
                    .counterparty(Optional.empty())
                    .flowId(TransactionFixtures.SOME_TRANSACTION_FLOW_ID)
                    .eventId("evt")
                    .correlationId("corr")
                    .traceId("trace")
                    .eventTime(TransactionFixtures.SOME_EVENT_TIME)
                    .ingestTime(TransactionFixtures.SOME_INGEST_TIME)
                    .typedFields(Map.of())
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  // ---------- Flow ----------

  @Test
  void flow_builder_buildsExpectedFlow() {
    // given
    var expected =
        new Flow(
            FlowFixtures.SOME_FLOW_ID,
            "MULTI_LEG",
            "IN_PROGRESS",
            FlowFixtures.SOME_FLOW_CUSTOMER_ID,
            MoneyFixtures.SOME_MONEY,
            3,
            FlowFixtures.SOME_FLOW_CREATED_AT,
            FlowFixtures.SOME_FLOW_UPDATED_AT,
            Optional.empty());

    // when
    var actual = FlowFixtures.SOME_FLOW;

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void flow_nullStatus_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                Flow.builder()
                    .id(FlowFixtures.SOME_FLOW_ID)
                    .flowType("MULTI_LEG")
                    .status(null)
                    .customerId(FlowFixtures.SOME_FLOW_CUSTOMER_ID)
                    .totalAmount(MoneyFixtures.SOME_MONEY)
                    .legCount(3)
                    .createdAt(FlowFixtures.SOME_FLOW_CREATED_AT)
                    .updatedAt(FlowFixtures.SOME_FLOW_UPDATED_AT)
                    .completedAt(Optional.empty())
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("status");
  }

  // ---------- DlqEvent ----------

  @Test
  void dlqEvent_builder_buildsExpectedDlqEvent() {
    // given
    var expected =
        new DlqEvent(
            DlqEventFixtures.SOME_DLQ_ID,
            "DeserializationException",
            "crypto.payin.events",
            2,
            12345L,
            "Failed to deserialize Avro payload",
            DlqEventFixtures.SOME_DLQ_FAILED_AT,
            1,
            Optional.of("opensearch"),
            Optional.of(DlqEventFixtures.SOME_DLQ_WATERMARK_AT),
            Optional.of("{\"reference\":\"TXN-REF-9999\"}"));

    // when
    var actual = DlqEventFixtures.SOME_DLQ_EVENT;

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void dlqEvent_nullErrorClass_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                DlqEvent.builder()
                    .id(DlqEventFixtures.SOME_DLQ_ID)
                    .errorClass(null)
                    .sourceTopic("topic")
                    .sourcePartition(0)
                    .sourceOffset(0L)
                    .errorMessage("err")
                    .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
                    .retryCount(0)
                    .sinkType(Optional.empty())
                    .watermarkAt(Optional.empty())
                    .originalPayloadJson(Optional.empty())
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("errorClass");
  }

  // ---------- StuckPayment ----------

  @Test
  void stuckPayment_builder_buildsExpectedStuckPayment() {
    // given
    var expected =
        new StuckPayment(
            StuckPaymentFixtures.SOME_STUCK_TRANSACTION_ID,
            "TXN-REF-STUCK-0001",
            "FIAT_PAYOUT",
            "AWAITING_CONFIRMATION",
            StuckPaymentFixtures.SOME_STUCK_CUSTOMER_ID,
            MoneyFixtures.SOME_MONEY,
            StuckPaymentFixtures.SOME_STUCK_LAST_EVENT_AT,
            900_000L);

    // when
    var actual = StuckPaymentFixtures.SOME_STUCK_PAYMENT;

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void stuckPayment_nullReference_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                StuckPayment.builder()
                    .id(StuckPaymentFixtures.SOME_STUCK_TRANSACTION_ID)
                    .reference(null)
                    .flowType("FIAT_PAYOUT")
                    .internalStatus("AWAITING_CONFIRMATION")
                    .customerId(StuckPaymentFixtures.SOME_STUCK_CUSTOMER_ID)
                    .amount(MoneyFixtures.SOME_MONEY)
                    .lastEventAt(StuckPaymentFixtures.SOME_STUCK_LAST_EVENT_AT)
                    .stuckMillis(0L)
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("reference");
  }

  // ---------- CustomerSummary ----------

  @Test
  void customerSummary_builder_buildsExpectedCustomerSummary() {
    // given
    var expected =
        new CustomerSummary(
            CustomerSummaryFixtures.SOME_CUSTOMER_ID,
            "Acme Corp",
            "masked-customer@example.com",
            "VERIFIED",
            MoneyFixtures.SOME_MONEY,
            MoneyFixtures.SOME_MONEY,
            42,
            CustomerSummaryFixtures.SOME_CUSTOMER_JOINED,
            "LOW");

    // when
    var actual = CustomerSummaryFixtures.SOME_CUSTOMER_SUMMARY;

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void customerSummary_nullEmail_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                CustomerSummary.builder()
                    .id(CustomerSummaryFixtures.SOME_CUSTOMER_ID)
                    .name("Acme")
                    .email(null)
                    .kyc("VERIFIED")
                    .balance(MoneyFixtures.SOME_MONEY)
                    .totalSent(MoneyFixtures.SOME_MONEY)
                    .txnCount(0)
                    .joined(CustomerSummaryFixtures.SOME_CUSTOMER_JOINED)
                    .risk("LOW")
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("email");
  }

  // ---------- PaginatedResult ----------

  @Test
  void paginatedResult_builder_buildsExpectedPaginatedResult() {
    // given
    var expected = new PaginatedResult<String>(List.of("a", "b"), Optional.of("cursor-1"));

    // when
    var actual =
        PaginatedResult.<String>builder()
            .items(List.of("a", "b"))
            .nextCursor(Optional.of("cursor-1"))
            .build();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void paginatedResult_nullItems_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> new PaginatedResult<String>(null, Optional.empty()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("items");
  }

  @Test
  void paginatedResult_defensiveCopy_originalMutated_resultUnchanged() {
    // given
    var mutable = new ArrayList<String>();
    mutable.add("a");
    mutable.add("b");
    var result = new PaginatedResult<String>(mutable, Optional.empty());
    mutable.add("c");

    // when
    var actualSize = result.items().size();

    // then
    assertThat(actualSize).isEqualTo(2);
  }

  @Test
  void paginatedResult_itemsList_isUnmodifiable() {
    // given
    var result = new PaginatedResult<String>(new ArrayList<>(List.of("a")), Optional.empty());

    // when / then
    assertThatThrownBy(() -> result.items().add("b"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // ---------- TransactionSearch ----------

  @Test
  void transactionSearch_builder_buildsExpectedTransactionSearch() {
    // given
    var from = Instant.parse("2026-05-01T00:00:00Z");
    var to = Instant.parse("2026-05-02T00:00:00Z");
    var expected =
        new TransactionSearch(
            Optional.of("TXN-REF-0001"),
            Optional.of("CRYPTO_PAYIN"),
            Optional.of("CONFIRMED"),
            Optional.of("COMPLETED"),
            Optional.of(from),
            Optional.of(to),
            50,
            Optional.of("cursor-1"));

    // when
    var actual =
        TransactionSearch.builder()
            .reference(Optional.of("TXN-REF-0001"))
            .flowType(Optional.of("CRYPTO_PAYIN"))
            .internalStatus(Optional.of("CONFIRMED"))
            .customerStatus(Optional.of("COMPLETED"))
            .from(Optional.of(from))
            .to(Optional.of(to))
            .pageSize(50)
            .cursor(Optional.of("cursor-1"))
            .build();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void transactionSearch_nullCursor_throwsNpe() {
    // when / then
    assertThatThrownBy(
            () ->
                TransactionSearch.builder()
                    .reference(Optional.empty())
                    .flowType(Optional.empty())
                    .internalStatus(Optional.empty())
                    .customerStatus(Optional.empty())
                    .from(Optional.empty())
                    .to(Optional.empty())
                    .pageSize(10)
                    .cursor(null)
                    .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("cursor");
  }

  // ---------- CachedResponse ----------

  @Test
  void cachedResponse_builder_buildsExpectedCachedResponse() {
    // given
    var expiresAt = Instant.parse("2026-05-01T11:00:00Z");
    var expected = new CachedResponse(200, new byte[] {1, 2, 3}, expiresAt);

    // when
    var actual = new CachedResponse(200, new byte[] {1, 2, 3}, expiresAt);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void cachedResponse_nullBody_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> new CachedResponse(200, null, Instant.parse("2026-05-01T11:00:00Z")))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("body");
  }

  @Test
  void cachedResponse_defensiveCopy_originalBytesMutated_storedBodyUnchanged() {
    // given
    var bytes = new byte[] {1, 2, 3};
    var cached = new CachedResponse(200, bytes, Instant.parse("2026-05-01T11:00:00Z"));
    bytes[0] = 99;

    // when
    var actualFirstByte = cached.body()[0];

    // then
    assertThat(actualFirstByte).isEqualTo((byte) 1);
  }

  @Test
  void cachedResponse_accessor_returnsDefensiveCopy_callerMutationDoesNotAffectStoredBody() {
    // given
    var cached =
        new CachedResponse(200, new byte[] {1, 2, 3}, Instant.parse("2026-05-01T11:00:00Z"));
    var firstAccess = cached.body();
    firstAccess[0] = 99;

    // when
    var actualFirstByteOnReread = cached.body()[0];

    // then
    assertThat(actualFirstByteOnReread).isEqualTo((byte) 1);
  }

  @Test
  void cachedResponse_statusBelowMin_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () -> new CachedResponse(99, new byte[] {1}, Instant.parse("2026-05-01T11:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("status");
  }

  @Test
  void cachedResponse_statusAboveMax_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () -> new CachedResponse(600, new byte[] {1}, Instant.parse("2026-05-01T11:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("status");
  }

  // ---------- Range validations ----------

  @Test
  void dlqEvent_negativeSourcePartition_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                DlqEvent.builder()
                    .id(DlqEventFixtures.SOME_DLQ_ID)
                    .errorClass("err")
                    .sourceTopic("topic")
                    .sourcePartition(-1)
                    .sourceOffset(0L)
                    .errorMessage("err")
                    .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
                    .retryCount(0)
                    .sinkType(Optional.empty())
                    .watermarkAt(Optional.empty())
                    .originalPayloadJson(Optional.empty())
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sourcePartition");
  }

  @Test
  void dlqEvent_negativeSourceOffset_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                DlqEvent.builder()
                    .id(DlqEventFixtures.SOME_DLQ_ID)
                    .errorClass("err")
                    .sourceTopic("topic")
                    .sourcePartition(0)
                    .sourceOffset(-1L)
                    .errorMessage("err")
                    .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
                    .retryCount(0)
                    .sinkType(Optional.empty())
                    .watermarkAt(Optional.empty())
                    .originalPayloadJson(Optional.empty())
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sourceOffset");
  }

  @Test
  void dlqEvent_negativeRetryCount_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                DlqEvent.builder()
                    .id(DlqEventFixtures.SOME_DLQ_ID)
                    .errorClass("err")
                    .sourceTopic("topic")
                    .sourcePartition(0)
                    .sourceOffset(0L)
                    .errorMessage("err")
                    .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
                    .retryCount(-1)
                    .sinkType(Optional.empty())
                    .watermarkAt(Optional.empty())
                    .originalPayloadJson(Optional.empty())
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("retryCount");
  }

  @Test
  void flow_zeroLegCount_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                Flow.builder()
                    .id(FlowFixtures.SOME_FLOW_ID)
                    .flowType("MULTI_LEG")
                    .status("IN_PROGRESS")
                    .customerId(FlowFixtures.SOME_FLOW_CUSTOMER_ID)
                    .totalAmount(MoneyFixtures.SOME_MONEY)
                    .legCount(0)
                    .createdAt(FlowFixtures.SOME_FLOW_CREATED_AT)
                    .updatedAt(FlowFixtures.SOME_FLOW_UPDATED_AT)
                    .completedAt(Optional.empty())
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("legCount");
  }

  @Test
  void customerSummary_negativeTxnCount_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                CustomerSummary.builder()
                    .id(CustomerSummaryFixtures.SOME_CUSTOMER_ID)
                    .name("Acme")
                    .email("e@x")
                    .kyc("VERIFIED")
                    .balance(MoneyFixtures.SOME_MONEY)
                    .totalSent(MoneyFixtures.SOME_MONEY)
                    .txnCount(-1)
                    .joined(CustomerSummaryFixtures.SOME_CUSTOMER_JOINED)
                    .risk("LOW")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("txnCount");
  }

  @Test
  void stuckPayment_negativeStuckMillis_throwsIllegalArgumentException() {
    // when / then
    assertThatThrownBy(
            () ->
                StuckPayment.builder()
                    .id(StuckPaymentFixtures.SOME_STUCK_TRANSACTION_ID)
                    .reference("ref")
                    .flowType("FIAT_PAYOUT")
                    .internalStatus("AWAITING_CONFIRMATION")
                    .customerId(StuckPaymentFixtures.SOME_STUCK_CUSTOMER_ID)
                    .amount(MoneyFixtures.SOME_MONEY)
                    .lastEventAt(StuckPaymentFixtures.SOME_STUCK_LAST_EVENT_AT)
                    .stuckMillis(-1L)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("stuckMillis");
  }

  // ---------- ID null validation ----------

  @Test
  void transactionId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> TransactionId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void flowId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> FlowId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void customerId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> CustomerId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void accountId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> AccountId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void dlqId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> DlqId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }

  @Test
  void userId_null_throwsNpe() {
    // when / then
    assertThatThrownBy(() -> UserId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value");
  }
}
