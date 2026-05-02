package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.AccountId;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TransactionFixtures {

  public static final TransactionId SOME_TRANSACTION_ID =
      TransactionId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

  public static final String SOME_REFERENCE = "TXN-REF-0001";

  public static final CustomerId SOME_TRANSACTION_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

  public static final AccountId SOME_TRANSACTION_ACCOUNT_ID =
      AccountId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));

  public static final FlowId SOME_TRANSACTION_FLOW_ID =
      FlowId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));

  public static final Instant SOME_EVENT_TIME = Instant.parse("2026-05-01T10:00:00Z");

  public static final Instant SOME_INGEST_TIME = Instant.parse("2026-05-01T10:00:01Z");

  public static final Map<String, Object> SOME_TYPED_FIELDS =
      Map.of("chain", "ethereum", "asset", "USDC");

  public static final Transaction SOME_TRANSACTION =
      Transaction.builder()
          .id(SOME_TRANSACTION_ID)
          .reference(SOME_REFERENCE)
          .flowType("CRYPTO_PAYIN")
          .internalStatus("CONFIRMED")
          .customerStatus("COMPLETED")
          .amount(MoneyFixtures.SOME_MONEY)
          .customerId(SOME_TRANSACTION_CUSTOMER_ID)
          .accountId(SOME_TRANSACTION_ACCOUNT_ID)
          .counterparty(Optional.of("0xabcdef"))
          .flowId(SOME_TRANSACTION_FLOW_ID)
          .eventId("evt-0001")
          .correlationId("corr-0001")
          .traceId("trace-0001")
          .eventTime(SOME_EVENT_TIME)
          .ingestTime(SOME_INGEST_TIME)
          .typedFields(SOME_TYPED_FIELDS)
          .build();

  private TransactionFixtures() {}

  public static Transaction.TransactionBuilder someTransaction() {
    return SOME_TRANSACTION.toBuilder();
  }
}
