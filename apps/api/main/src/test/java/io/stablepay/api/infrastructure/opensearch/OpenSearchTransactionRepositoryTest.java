package io.stablepay.api.infrastructure.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.AccountId;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.model.TransactionSearch;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.fixtures.OpenSearchTransactionDocumentFixtures;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

@ExtendWith(MockitoExtension.class)
class OpenSearchTransactionRepositoryTest {

  private static final String INDEX_NAME = "transactions";

  private OpenSearchClient client;
  private OpenSearchDocumentMapper mapper;
  private OpenSearchTransactionRepository repository;

  @BeforeEach
  void setUp() {
    client = mock(OpenSearchClient.class);
    mapper = mock(OpenSearchDocumentMapper.class);
    repository = new OpenSearchTransactionRepository(client, mapper, INDEX_NAME);
  }

  @Test
  void shouldReturnTransactionWhenFindByReferenceMatchesCustomerScope() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual =
        repository.findByReference(
            document.transactionReference(), TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    var expected = Optional.of(domain);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturnEmptyWhenFindByReferenceFindsNoHits() throws IOException {
    // given
    var response = searchResponseWith(List.of());
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);

    // when
    var actual =
        repository.findByReference("missing", TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.empty());
  }

  @Test
  void shouldReturnTransactionWhenFindByReferenceAdminMatchesAnyCustomer() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual = repository.findByReferenceAdmin(document.transactionReference());

    // then
    var expected = Optional.of(domain);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturnPaginatedSearchResultScopedToCustomer() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);
    var criteria = pageOfTen();

    // when
    var actual = repository.search(criteria, TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    var expected = new PaginatedResult<>(List.of(domain), Optional.<String>empty());
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturnPaginatedSearchResultForAdminWithoutCustomerScope() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);
    var criteria = pageOfTen();

    // when
    var actual = repository.searchAdmin(criteria);

    // then
    var expected = new PaginatedResult<>(List.of(domain), Optional.<String>empty());
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldEncodeNextCursorWhenPageIsFull() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);
    var criteria =
        TransactionSearch.builder()
            .reference(Optional.empty())
            .flowType(Optional.empty())
            .internalStatus(Optional.empty())
            .customerStatus(Optional.empty())
            .from(Optional.empty())
            .to(Optional.empty())
            .pageSize(1)
            .cursor(Optional.empty())
            .build();
    var expectedCursor =
        OpenSearchCursorCodec.encode(document.eventTimeEpochMillis(), document.eventId());

    // when
    var actual = repository.searchAdmin(criteria);

    // then
    var expected = new PaginatedResult<>(List.of(domain), Optional.of(expectedCursor));
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldStreamTailBatchInSortOrder() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = mappedDomain(document);
    var response = searchResponseWith(List.of(document));
    given(client.search(any(SearchRequest.class), any(Class.class))).willReturn(response);
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual = repository.tailSinceSortValue(Optional.empty(), 100).toList();

    // then
    var expected = List.of(domain);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldWrapIOExceptionAsAdapterException() throws IOException {
    // given
    given(client.search(any(SearchRequest.class), any(Class.class)))
        .willThrow(new IOException("boom"));

    // when / then
    assertThatThrownBy(
            () ->
                repository.findByReference(
                    "TXN-REF", TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID))
        .isInstanceOf(OpenSearchAdapterException.class)
        .hasMessageContaining("STBLPAY-2001");
  }

  private TransactionSearch pageOfTen() {
    return TransactionSearch.builder()
        .reference(Optional.empty())
        .flowType(Optional.empty())
        .internalStatus(Optional.empty())
        .customerStatus(Optional.empty())
        .from(Optional.empty())
        .to(Optional.empty())
        .pageSize(10)
        .cursor(Optional.empty())
        .build();
  }

  private static Transaction mappedDomain(OpenSearchTransactionDocument document) {
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

  @SuppressWarnings("unchecked")
  private static SearchResponse<OpenSearchTransactionDocument> searchResponseWith(
      List<OpenSearchTransactionDocument> docs) {
    var response = (SearchResponse<OpenSearchTransactionDocument>) mock(SearchResponse.class);
    var hitsMeta = (HitsMetadata<OpenSearchTransactionDocument>) mock(HitsMetadata.class);
    var hitList =
        docs.stream()
            .map(
                doc ->
                    Hit.<OpenSearchTransactionDocument>of(
                        h ->
                            h.index("transactions")
                                .id(doc.eventId())
                                .score(1.0)
                                .source(doc)
                                .sort(
                                    List.of(
                                        org.opensearch.client.opensearch._types.FieldValue.of(
                                            doc.eventTimeEpochMillis()),
                                        org.opensearch.client.opensearch._types.FieldValue.of(
                                            doc.eventId())))))
            .toList();
    given(response.hits()).willReturn(hitsMeta);
    given(hitsMeta.hits()).willReturn(hitList);
    return response;
  }
}
