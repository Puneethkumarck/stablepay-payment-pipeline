package io.stablepay.api.infrastructure.opensearch;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionSearch;
import io.stablepay.api.domain.port.TransactionRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OpenSearchTransactionRepository implements TransactionRepository {

  private static final String FIELD_TRANSACTION_REFERENCE = "transaction_reference";
  private static final String FIELD_CUSTOMER_ID = "customer_id";
  private static final String FIELD_FLOW_TYPE = "flow_type";
  private static final String FIELD_INTERNAL_STATUS = "internal_status";
  private static final String FIELD_CUSTOMER_STATUS = "customer_status";
  private static final String FIELD_EVENT_TIME = "event_time";
  private static final String FIELD_EVENT_ID = "event_id";

  private final OpenSearchClient client;
  private final OpenSearchDocumentMapper mapper;
  private final OpenSearchProperties properties;

  @Override
  public Optional<Transaction> findByReference(String reference, CustomerId customerId) {
    var request =
        SearchRequest.of(
            r ->
                r.index(properties.transactionsIndex())
                    .size(1)
                    .query(
                        q ->
                            q.bool(
                                b ->
                                    b.must(
                                            m ->
                                                m.term(
                                                    t ->
                                                        t.field(FIELD_TRANSACTION_REFERENCE)
                                                            .value(FieldValue.of(reference))))
                                        .filter(
                                            f ->
                                                f.term(
                                                    t ->
                                                        t.field(FIELD_CUSTOMER_ID)
                                                            .value(
                                                                FieldValue.of(
                                                                    customerId
                                                                        .value()
                                                                        .toString())))))));
    return executeSingle(request);
  }

  @Override
  public Optional<Transaction> findByReferenceAdmin(String reference) {
    var request =
        SearchRequest.of(
            r ->
                r.index(properties.transactionsIndex())
                    .size(1)
                    .query(
                        q ->
                            q.term(
                                t ->
                                    t.field(FIELD_TRANSACTION_REFERENCE)
                                        .value(FieldValue.of(reference)))));
    return executeSingle(request);
  }

  @Override
  public PaginatedResult<Transaction> search(TransactionSearch criteria, CustomerId customerId) {
    return executePaginated(criteria, Optional.of(customerId));
  }

  @Override
  public PaginatedResult<Transaction> searchAdmin(TransactionSearch criteria) {
    return executePaginated(criteria, Optional.empty());
  }

  @Override
  public Stream<Transaction> tailSinceSortValueAdmin(Optional<String> sortValue, int batchSize) {
    var request =
        SearchRequest.of(
            r -> {
              r.index(properties.transactionsIndex())
                  .size(batchSize)
                  .query(Query.of(q -> q.matchAll(m -> m)));
              r.sort(s -> s.field(f -> f.field(FIELD_EVENT_TIME).order(SortOrder.Asc)))
                  .sort(s -> s.field(f -> f.field(FIELD_EVENT_ID).order(SortOrder.Asc)));
              sortValue
                  .map(OpenSearchCursorCodec::decode)
                  .ifPresent(
                      cursor ->
                          r.searchAfter(
                              List.of(
                                  FieldValue.of(cursor.eventTimeMillis()),
                                  FieldValue.of(cursor.eventId()))));
              return r;
            });
    try {
      var response = client.search(request, OpenSearchTransactionDocument.class);
      return response.hits().hits().stream().map(Hit::source).map(mapper::toDomain);
    } catch (IOException | OpenSearchException e) {
      throw new OpenSearchAdapterException(e);
    }
  }

  private PaginatedResult<Transaction> executePaginated(
      TransactionSearch criteria, Optional<CustomerId> customerId) {
    var pageSize = criteria.pageSize();
    var request =
        SearchRequest.of(
            r -> {
              r.index(properties.transactionsIndex())
                  .size(pageSize + 1)
                  .query(q -> q.bool(b -> applyFilters(b, criteria, customerId)))
                  .sort(s -> s.field(f -> f.field(FIELD_EVENT_TIME).order(SortOrder.Asc)))
                  .sort(s -> s.field(f -> f.field(FIELD_EVENT_ID).order(SortOrder.Asc)));
              criteria
                  .cursor()
                  .map(OpenSearchCursorCodec::decode)
                  .ifPresent(
                      cursor ->
                          r.searchAfter(
                              List.of(
                                  FieldValue.of(cursor.eventTimeMillis()),
                                  FieldValue.of(cursor.eventId()))));
              return r;
            });
    try {
      var response = client.search(request, OpenSearchTransactionDocument.class);
      var hits = response.hits().hits();
      if (hits.size() <= pageSize) {
        var items = hits.stream().map(Hit::source).map(mapper::toDomain).toList();
        return new PaginatedResult<>(items, Optional.empty());
      }
      var keptHits = hits.subList(0, pageSize);
      var items = keptHits.stream().map(Hit::source).map(mapper::toDomain).toList();
      var lastSource = keptHits.get(keptHits.size() - 1).source();
      var nextCursor =
          OpenSearchCursorCodec.encode(lastSource.eventTimeEpochMillis(), lastSource.eventId());
      return new PaginatedResult<>(items, Optional.of(nextCursor));
    } catch (IOException | OpenSearchException e) {
      throw new OpenSearchAdapterException(e);
    }
  }

  private BoolQuery.Builder applyFilters(
      BoolQuery.Builder builder, TransactionSearch criteria, Optional<CustomerId> customerId) {
    customerId.ifPresent(
        scope ->
            builder.filter(
                f ->
                    f.term(
                        t ->
                            t.field(FIELD_CUSTOMER_ID)
                                .value(FieldValue.of(scope.value().toString())))));
    criteria
        .reference()
        .ifPresent(
            ref ->
                builder.filter(
                    f ->
                        f.term(
                            t -> t.field(FIELD_TRANSACTION_REFERENCE).value(FieldValue.of(ref)))));
    criteria
        .flowType()
        .ifPresent(
            ft ->
                builder.filter(
                    f -> f.term(t -> t.field(FIELD_FLOW_TYPE).value(FieldValue.of(ft)))));
    criteria
        .internalStatus()
        .ifPresent(
            is ->
                builder.filter(
                    f -> f.term(t -> t.field(FIELD_INTERNAL_STATUS).value(FieldValue.of(is)))));
    criteria
        .customerStatus()
        .ifPresent(
            cs ->
                builder.filter(
                    f -> f.term(t -> t.field(FIELD_CUSTOMER_STATUS).value(FieldValue.of(cs)))));
    if (criteria.from().isPresent() || criteria.to().isPresent()) {
      builder.filter(
          f ->
              f.range(
                  rg -> {
                    var range = rg.field(FIELD_EVENT_TIME);
                    criteria.from().ifPresent(from -> range.gte(JsonData.of(from.toEpochMilli())));
                    criteria.to().ifPresent(to -> range.lte(JsonData.of(to.toEpochMilli())));
                    return range;
                  }));
    }
    return builder;
  }

  private Optional<Transaction> executeSingle(SearchRequest request) {
    try {
      var response = client.search(request, OpenSearchTransactionDocument.class);
      return response.hits().hits().stream().findFirst().map(Hit::source).map(mapper::toDomain);
    } catch (IOException | OpenSearchException e) {
      throw new OpenSearchAdapterException(e);
    }
  }
}
