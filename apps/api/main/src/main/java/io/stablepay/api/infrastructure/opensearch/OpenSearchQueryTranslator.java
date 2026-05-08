package io.stablepay.api.infrastructure.opensearch;

import io.stablepay.api.domain.agent.AggShape;
import io.stablepay.api.domain.agent.QueryShape;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchQueryTranslator {

  public Query translateQuery(QueryShape shape) {
    return switch (shape) {
      case QueryShape.TermShape term -> translateTerm(term);
      case QueryShape.RangeShape range -> translateRange(range);
      case QueryShape.MatchPhraseShape matchPhrase -> translateMatchPhrase(matchPhrase);
      case QueryShape.ExistsShape exists -> translateExists(exists);
      case QueryShape.TermsShape terms -> translateTerms(terms);
      case QueryShape.BoolShape bool -> translateBool(bool);
    };
  }

  public Map<String, Aggregation> translateAggs(List<AggShape> aggs) {
    var result = new HashMap<String, Aggregation>();
    for (var agg : aggs) {
      result.put(aggName(agg), translateAgg(agg));
    }
    return Map.copyOf(result);
  }

  private Query translateTerm(QueryShape.TermShape term) {
    return new Query.Builder()
        .term(t -> t.field(term.field()).value(FieldValue.of(term.value().toString())))
        .build();
  }

  private Query translateRange(QueryShape.RangeShape range) {
    return new Query.Builder()
        .range(
            r -> {
              var builder = r.field(range.field());
              range.gte().ifPresent(v -> builder.gte(toJsonData(v)));
              range.lte().ifPresent(v -> builder.lte(toJsonData(v)));
              return builder;
            })
        .build();
  }

  private Query translateMatchPhrase(QueryShape.MatchPhraseShape matchPhrase) {
    return new Query.Builder()
        .matchPhrase(mp -> mp.field(matchPhrase.field()).query(matchPhrase.phrase()))
        .build();
  }

  private Query translateExists(QueryShape.ExistsShape exists) {
    return new Query.Builder().exists(e -> e.field(exists.field())).build();
  }

  private Query translateTerms(QueryShape.TermsShape terms) {
    var fieldValues = terms.values().stream().map(v -> FieldValue.of(v.toString())).toList();
    return new Query.Builder()
        .terms(t -> t.field(terms.field()).terms(tv -> tv.value(fieldValues)))
        .build();
  }

  private Query translateBool(QueryShape.BoolShape bool) {
    var builder = new BoolQuery.Builder();
    for (var clause : bool.must()) {
      builder.must(translateQuery(clause));
    }
    for (var clause : bool.filter()) {
      builder.filter(translateQuery(clause));
    }
    for (var clause : bool.mustNot()) {
      builder.mustNot(translateQuery(clause));
    }
    return new Query.Builder().bool(builder.build()).build();
  }

  private Aggregation translateAgg(AggShape agg) {
    return switch (agg) {
      case AggShape.TermsAggShape a ->
          new Aggregation.Builder().terms(t -> t.field(a.field()).size(a.size())).build();
      case AggShape.DateHistogramAggShape a ->
          new Aggregation.Builder()
              .dateHistogram(
                  dh ->
                      dh.field(a.field())
                          .calendarInterval(toCalendarInterval(a.calendarInterval())))
              .build();
      case AggShape.SumAggShape a -> new Aggregation.Builder().sum(s -> s.field(a.field())).build();
      case AggShape.AvgAggShape a ->
          new Aggregation.Builder().avg(av -> av.field(a.field())).build();
    };
  }

  private static String aggName(AggShape agg) {
    return switch (agg) {
      case AggShape.TermsAggShape a -> a.name();
      case AggShape.DateHistogramAggShape a -> a.name();
      case AggShape.SumAggShape a -> a.name();
      case AggShape.AvgAggShape a -> a.name();
    };
  }

  private static CalendarInterval toCalendarInterval(String interval) {
    return CalendarInterval.valueOf(
        interval.substring(0, 1).toUpperCase(Locale.ROOT)
            + interval.substring(1).toLowerCase(Locale.ROOT));
  }

  private static JsonData toJsonData(Object value) {
    return JsonData.of(value);
  }
}
