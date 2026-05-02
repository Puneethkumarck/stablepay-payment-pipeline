package io.stablepay.api.infrastructure.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.TransactionSearch;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.fixtures.OpenSearchTransactionDocumentFixtures;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
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
  private ArgumentCaptor<SearchRequest> requestCaptor;

  @BeforeEach
  void setUp() {
    client = mock(OpenSearchClient.class);
    mapper = mock(OpenSearchDocumentMapper.class);
    var properties =
        OpenSearchProperties.builder().uri("http://test").transactionsIndex(INDEX_NAME).build();
    repository = new OpenSearchTransactionRepository(client, mapper, properties);
    requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
  }

  @Test
  void shouldReturnTransactionWhenFindByReferenceMatchesCustomerScope() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual =
        repository.findByReference(
            document.transactionReference(), TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    var expected = Optional.of(domain);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    var sentRequest = requestCaptor.getValue();
    assertThat(sentRequest.index()).containsExactly(INDEX_NAME);
    assertThat(sentRequest.size()).isEqualTo(1);
    assertThat(sentRequest.query().isBool()).isTrue();
    var bool = sentRequest.query().bool();
    assertThat(bool.must())
        .singleElement()
        .satisfies(
            q -> {
              assertThat(q.isTerm()).isTrue();
              assertThat(q.term().field()).isEqualTo("transaction_reference");
              assertThat(q.term().value().stringValue()).isEqualTo(document.transactionReference());
            });
    assertThat(bool.filter())
        .singleElement()
        .satisfies(
            q -> {
              assertThat(q.isTerm()).isTrue();
              assertThat(q.term().field()).isEqualTo("customer_id");
              assertThat(q.term().value().stringValue())
                  .isEqualTo(TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID.value().toString());
            });
  }

  @Test
  void shouldReturnEmptyWhenFindByReferenceFindsNoHits() throws IOException {
    // given
    stubSearch(List.of());

    // when
    var actual =
        repository.findByReference("missing", TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.empty());
  }

  @Test
  void shouldOmitCustomerFilterOnFindByReferenceAdmin() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual = repository.findByReferenceAdmin(document.transactionReference());

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(Optional.of(domain));
    var sent = requestCaptor.getValue();
    assertThat(sent.query().isTerm()).isTrue();
    assertThat(sent.query().term().field()).isEqualTo("transaction_reference");
    assertThat(sent.query().isBool()).isFalse();
  }

  @Test
  void shouldReturnPaginatedSearchResultScopedToCustomerWithOverfetch() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);
    var criteria = pageOfTen();

    // when
    var actual = repository.search(criteria, TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID);

    // then
    var expected =
        PaginatedResult.<io.stablepay.api.domain.model.Transaction>builder()
            .items(List.of(domain))
            .nextCursor(Optional.<String>empty())
            .build();
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    var sent = requestCaptor.getValue();
    assertThat(sent.size()).isEqualTo(criteria.pageSize() + 1);
    assertThat(sent.sort()).hasSize(2);
    assertThat(sent.sort().get(0).field().field()).isEqualTo("event_time");
    assertThat(sent.sort().get(0).field().order()).isEqualTo(SortOrder.Asc);
    assertThat(sent.sort().get(1).field().field()).isEqualTo("event_id");
    assertThat(sent.query().isBool()).isTrue();
    assertThat(sent.query().bool().filter())
        .anyMatch(
            q ->
                q.isTerm()
                    && q.term().field().equals("customer_id")
                    && q.term()
                        .value()
                        .stringValue()
                        .equals(
                            TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID.value().toString()));
  }

  @Test
  void shouldReturnPaginatedSearchResultForAdminWithoutCustomerScope() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);
    var criteria = pageOfTen();

    // when
    var actual = repository.searchAdmin(criteria);

    // then
    var expected =
        PaginatedResult.<io.stablepay.api.domain.model.Transaction>builder()
            .items(List.of(domain))
            .nextCursor(Optional.<String>empty())
            .build();
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    var sent = requestCaptor.getValue();
    assertThat(sent.query().bool().filter())
        .noneMatch(q -> q.isTerm() && q.term().field().equals("customer_id"));
  }

  @Test
  void shouldEncodeNextCursorWhenOverfetchYieldsMoreThanPageSize() throws IOException {
    // given
    var documentA = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var documentB =
        OpenSearchTransactionDocumentFixtures.someOpenSearchDocument()
            .eventId(documentA.eventId())
            .build();
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(documentA);
    stubSearch(List.of(documentA, documentB));
    given(mapper.toDomain(documentA)).willReturn(domain);
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
        OpenSearchCursorCodec.encode(documentA.eventTimeEpochMillis(), documentA.eventId());

    // when
    var actual = repository.searchAdmin(criteria);

    // then
    var expected =
        PaginatedResult.<io.stablepay.api.domain.model.Transaction>builder()
            .items(List.of(domain))
            .nextCursor(Optional.of(expectedCursor))
            .build();
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldOmitNextCursorWhenOverfetchHitsAreAtOrBelowPageSize() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
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

    // when
    var actual = repository.searchAdmin(criteria);

    // then
    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(
            PaginatedResult.<io.stablepay.api.domain.model.Transaction>builder()
                .items(List.of(domain))
                .nextCursor(Optional.<String>empty())
                .build());
  }

  @Test
  void shouldStreamTailBatchInSortOrder() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);

    // when
    var actual = repository.tailSinceSortValueAdmin(Optional.empty(), 100).toList();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(domain));
    var sent = requestCaptor.getValue();
    assertThat(sent.size()).isEqualTo(100);
    assertThat(sent.sort()).hasSize(2);
    assertThat(sent.sort().get(0).field().field()).isEqualTo("event_time");
    assertThat(sent.sort().get(1).field().field()).isEqualTo("event_id");
    assertThat(sent.searchAfter()).isEmpty();
  }

  @Test
  void shouldUseSearchAfterWhenTailHasCursor() throws IOException {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var domain = OpenSearchTransactionDocumentFixtures.toMappedDomain(document);
    stubSearch(List.of(document));
    given(mapper.toDomain(document)).willReturn(domain);
    var cursor = OpenSearchCursorCodec.encode(document.eventTimeEpochMillis(), document.eventId());

    // when
    var actual = repository.tailSinceSortValueAdmin(Optional.of(cursor), 50).toList();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(domain));
    var sent = requestCaptor.getValue();
    assertThat(sent.searchAfter()).hasSize(2);
    assertThat(sent.searchAfter().get(0).longValue()).isEqualTo(document.eventTimeEpochMillis());
    assertThat(sent.searchAfter().get(1).stringValue()).isEqualTo(document.eventId());
  }

  @Test
  void shouldWrapIOExceptionAsAdapterException() throws IOException {
    // given
    given(client.search(any(SearchRequest.class), eq(OpenSearchTransactionDocument.class)))
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

  private void stubSearch(List<OpenSearchTransactionDocument> docs) throws IOException {
    var response = searchResponseWith(docs);
    given(client.search(requestCaptor.capture(), eq(OpenSearchTransactionDocument.class)))
        .willReturn(response);
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
                                        FieldValue.of(doc.eventTimeEpochMillis()),
                                        FieldValue.of(doc.eventId())))))
            .toList();
    given(response.hits()).willReturn(hitsMeta);
    given(hitsMeta.hits()).willReturn(hitList);
    return response;
  }
}
