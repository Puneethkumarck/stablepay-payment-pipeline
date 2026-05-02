package io.stablepay.api.infrastructure.opensearch;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.AccountId;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link OpenSearchTransactionDocument} (wire format) into the {@link Transaction} domain
 * record. The reverse direction is intentionally omitted in Phase 4.2.3 — the API never writes to
 * OpenSearch; Flink sinks own that path.
 *
 * <p>{@code counterparty} and {@code typedFields} default to empty/empty-map: the deployed index
 * template has no single field to source them from, and the future event-extraction layer will
 * populate them once richer payloads are indexed.
 */
@Mapper(
    componentModel = "spring",
    imports = {
      Money.class,
      CurrencyCode.class,
      TransactionId.class,
      CustomerId.class,
      AccountId.class,
      FlowId.class,
      Instant.class,
      Optional.class,
      Map.class,
      UUID.class
    })
public interface OpenSearchDocumentMapper {

  @Mapping(target = "id", expression = "java(TransactionId.of(UUID.fromString(doc.eventId())))")
  @Mapping(target = "reference", expression = "java(doc.transactionReference())")
  @Mapping(target = "flowType", expression = "java(doc.flowType())")
  @Mapping(target = "internalStatus", expression = "java(doc.internalStatus())")
  @Mapping(target = "customerStatus", expression = "java(doc.customerStatus())")
  @Mapping(
      target = "amount",
      expression =
          "java(Money.fromMicros(doc.amountMicros(), CurrencyCode.getByCode(doc.currencyCode())))")
  @Mapping(
      target = "customerId",
      expression = "java(CustomerId.of(UUID.fromString(doc.customerId())))")
  @Mapping(
      target = "accountId",
      expression = "java(AccountId.of(UUID.fromString(doc.accountId())))")
  @Mapping(target = "counterparty", expression = "java(Optional.<String>empty())")
  @Mapping(target = "flowId", expression = "java(FlowId.of(UUID.fromString(doc.flowId())))")
  @Mapping(target = "eventId", expression = "java(doc.eventId())")
  @Mapping(target = "correlationId", expression = "java(doc.correlationId())")
  @Mapping(target = "traceId", expression = "java(doc.traceId())")
  @Mapping(
      target = "eventTime",
      expression = "java(Instant.ofEpochMilli(doc.eventTimeEpochMillis()))")
  @Mapping(
      target = "ingestTime",
      expression = "java(Instant.ofEpochMilli(doc.ingestTimeEpochMillis()))")
  @Mapping(target = "typedFields", expression = "java(Map.<String, Object>of())")
  Transaction toDomain(OpenSearchTransactionDocument doc);
}
