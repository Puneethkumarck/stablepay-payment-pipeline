package io.stablepay.api.infrastructure.opensearch.fixtures;

import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.OpenSearchTransactionDocument;

/**
 * Canonical {@link OpenSearchTransactionDocument} fixtures pinned to {@link
 * TransactionFixtures#SOME_TRANSACTION}. The {@code event_id} string deliberately matches {@code
 * SOME_TRANSACTION.id().value()} so a doc indexed under {@code SOME_TRANSACTION_ID} round trips
 * back to the canonical fixture after mapping.
 */
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
}
