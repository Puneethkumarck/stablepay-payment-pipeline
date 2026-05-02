package io.stablepay.api.infrastructure.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.AccountId;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.fixtures.OpenSearchTransactionDocumentFixtures;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Round-trips a known document through a real single-node OpenSearch container. Wires the adapter
 * manually (no Spring context) per the standards' integration-test pyramid level.
 */
@Testcontainers
@Tag("integration")
class OpenSearchTransactionRepositoryIT {

  private static final String INDEX_NAME = "transactions";

  private static OpensearchContainer<?> container;
  private static OpenSearchClient client;
  private static OpenSearchTransactionRepository repository;

  @BeforeAll
  static void setup() throws Exception {
    container =
        new OpensearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:2.18.0"))
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m");
    container.start();
    var transport =
        ApacheHttpClient5TransportBuilder.builder(
                HttpHost.create(URI.create(container.getHttpHostAddress())))
            .setMapper(new JacksonJsonpMapper())
            .build();
    client = new OpenSearchClient(transport);
    var mapper = Mappers.getMapper(OpenSearchDocumentMapper.class);
    repository = new OpenSearchTransactionRepository(client, mapper, INDEX_NAME);
    createIndex();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (client != null) {
      client._transport().close();
    }
    if (container != null) {
      container.stop();
    }
  }

  @Test
  void shouldRoundTripDocumentThroughFindByReference() throws Exception {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    client.index(
        i ->
            i.index(INDEX_NAME).id(document.eventId()).document(document).refresh(Refresh.WaitFor));
    var expected =
        Optional.of(
            Transaction.builder()
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
                .build());

    // when
    var actual =
        repository.findByReference(
            document.transactionReference(), TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  private static void createIndex() throws Exception {
    client
        .indices()
        .create(
            c ->
                c.index(INDEX_NAME)
                    .mappings(
                        m ->
                            m.properties("event_id", p -> p.keyword(k -> k))
                                .properties(
                                    "event_time", p -> p.date(d -> d.format("epoch_millis")))
                                .properties(
                                    "ingest_time", p -> p.date(d -> d.format("epoch_millis")))
                                .properties("transaction_reference", p -> p.keyword(k -> k))
                                .properties("flow_type", p -> p.keyword(k -> k))
                                .properties("internal_status", p -> p.keyword(k -> k))
                                .properties("customer_status", p -> p.keyword(k -> k))
                                .properties("amount_micros", p -> p.long_(l -> l))
                                .properties("currency_code", p -> p.keyword(k -> k))
                                .properties("customer_id", p -> p.keyword(k -> k))
                                .properties("account_id", p -> p.keyword(k -> k))
                                .properties("flow_id", p -> p.keyword(k -> k))
                                .properties("correlation_id", p -> p.keyword(k -> k))
                                .properties("trace_id", p -> p.keyword(k -> k))));
  }
}
