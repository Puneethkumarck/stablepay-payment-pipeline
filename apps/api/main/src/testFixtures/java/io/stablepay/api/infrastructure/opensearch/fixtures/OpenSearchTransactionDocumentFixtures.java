package io.stablepay.api.infrastructure.opensearch.fixtures;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.AccountId;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.OpenSearchTransactionDocument;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class OpenSearchTransactionDocumentFixtures {

  public static final OpenSearchTransactionDocument SOME_OS_TRANSACTION_DOCUMENT =
      OpenSearchTransactionDocument.builder()
          .eventId(TransactionFixtures.SOME_TRANSACTION_ID.value().toString())
          .transactionReference(TransactionFixtures.SOME_REFERENCE)
          .flowType("CRYPTO_PAYIN")
          .internalStatus("CONFIRMED")
          .customerStatus("COMPLETED")
          .amountMicros(100_000_000L)
          .currencyCode("USD")
          .customerId(TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID.value().toString())
          .accountId(TransactionFixtures.SOME_TRANSACTION_ACCOUNT_ID.value().toString())
          .flowId(TransactionFixtures.SOME_TRANSACTION_FLOW_ID.value().toString())
          .correlationId("corr-0001")
          .traceId("trace-0001")
          .eventTimeEpochMillis(TransactionFixtures.SOME_EVENT_TIME.toEpochMilli())
          .ingestTimeEpochMillis(TransactionFixtures.SOME_INGEST_TIME.toEpochMilli())
          .build();

  private OpenSearchTransactionDocumentFixtures() {}

  public static OpenSearchTransactionDocument.OpenSearchTransactionDocumentBuilder
      someOpenSearchDocument() {
    return SOME_OS_TRANSACTION_DOCUMENT.toBuilder();
  }

  public static Transaction toMappedDomain(OpenSearchTransactionDocument document) {
    return Transaction.builder()
        .id(TransactionId.of(UUID.fromString(document.eventId())))
        .reference(document.transactionReference())
        .flowType(document.flowType())
        .internalStatus(document.internalStatus())
        .customerStatus(document.customerStatus())
        .amount(
            Money.fromMicros(
                document.amountMicros(), CurrencyCode.getByCode(document.currencyCode())))
        .customerId(CustomerId.of(UUID.fromString(document.customerId())))
        .accountId(AccountId.of(UUID.fromString(document.accountId())))
        .counterparty(Optional.<String>empty())
        .flowId(FlowId.of(UUID.fromString(document.flowId())))
        .eventId(document.eventId())
        .correlationId(document.correlationId())
        .traceId(document.traceId())
        .eventTime(Instant.ofEpochMilli(document.eventTimeEpochMillis()))
        .ingestTime(Instant.ofEpochMilli(document.ingestTimeEpochMillis()))
        .typedFields(Map.<String, Object>of())
        .build();
  }
}
