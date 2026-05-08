package io.stablepay.api.domain.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenSearchDslWhitelistValidator {

  static final int DEFAULT_SIZE = 100;
  static final int MAX_SIZE = 1_000;

  private final AllowedQueryShapeRegistry registry;

  public DslValidationResult validate(AgentSearchRequest request) {
    if (!registry.isIndexAllowed(request.index())) {
      return new DslValidationResult.Invalid(
          "Index not in allowlist: %s".formatted(request.index()), "STBLPAY-6001");
    }

    var fieldResult = validateFields(request.query());
    if (fieldResult != null) {
      return fieldResult;
    }

    if (request.sort().isPresent()) {
      for (var sortField : request.sort().get()) {
        if (!registry.isFieldAllowed(sortField)) {
          return new DslValidationResult.Invalid(
              "Sort field not in allowlist: %s".formatted(sortField), "STBLPAY-6005");
        }
      }
    }

    Map<String, Aggregation> translatedAggs = Map.of();
    if (request.aggs().isPresent()) {
      var aggResult = validateAndTranslateAggs(request.aggs().get());
      if (aggResult instanceof AggTranslation.InvalidAgg invalid) {
        return new DslValidationResult.Invalid(invalid.reason(), invalid.errorCode());
      }
      translatedAggs = ((AggTranslation.ValidAgg) aggResult).aggs();
    }

    var translatedQuery = translateQuery(request.query());
    var size = resolveSize(request.size());

    return new DslValidationResult.Valid(translatedQuery, translatedAggs, size);
  }

  private DslValidationResult.Invalid validateFields(QueryShape shape) {
    return switch (shape) {
      case QueryShape.TermShape term -> validateField(term.field());
      case QueryShape.RangeShape range -> validateField(range.field());
      case QueryShape.MatchPhraseShape matchPhrase -> validateField(matchPhrase.field());
      case QueryShape.ExistsShape exists -> validateField(exists.field());
      case QueryShape.TermsShape terms -> validateField(terms.field());
      case QueryShape.BoolShape bool -> validateBoolFields(bool);
    };
  }

  private DslValidationResult.Invalid validateBoolFields(QueryShape.BoolShape bool) {
    for (var clause : bool.must()) {
      var result = validateFields(clause);
      if (result != null) {
        return result;
      }
    }
    for (var clause : bool.filter()) {
      var result = validateFields(clause);
      if (result != null) {
        return result;
      }
    }
    for (var clause : bool.mustNot()) {
      var result = validateFields(clause);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private DslValidationResult.Invalid validateField(String field) {
    if (!registry.isFieldAllowed(field)) {
      return new DslValidationResult.Invalid(
          "Field not in allowlist: %s".formatted(field), "STBLPAY-6002");
    }
    return null;
  }

  private Query translateQuery(QueryShape shape) {
    return switch (shape) {
      case QueryShape.TermShape term -> translateTerm(term);
      case QueryShape.RangeShape range -> translateRange(range);
      case QueryShape.MatchPhraseShape matchPhrase -> translateMatchPhrase(matchPhrase);
      case QueryShape.ExistsShape exists -> translateExists(exists);
      case QueryShape.TermsShape terms -> translateTerms(terms);
      case QueryShape.BoolShape bool -> translateBool(bool);
    };
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
              range.gte().ifPresent(v -> builder.gte(toFieldValueJson(v)));
              range.lte().ifPresent(v -> builder.lte(toFieldValueJson(v)));
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

  private sealed interface AggTranslation
      permits AggTranslation.ValidAgg, AggTranslation.InvalidAgg {
    record ValidAgg(Map<String, Aggregation> aggs) implements AggTranslation {}

    record InvalidAgg(String reason, String errorCode) implements AggTranslation {}
  }

  private AggTranslation validateAndTranslateAggs(List<AggShape> aggs) {
    var result = new HashMap<String, Aggregation>();
    for (var agg : aggs) {
      var fieldCheck = validateAggField(agg);
      if (fieldCheck != null) {
        return new AggTranslation.InvalidAgg(fieldCheck.reason(), fieldCheck.errorCode());
      }
      var translated = translateAgg(agg);
      result.put(aggName(agg), translated);
    }
    return new AggTranslation.ValidAgg(Map.copyOf(result));
  }

  private DslValidationResult.Invalid validateAggField(AggShape agg) {
    var field =
        switch (agg) {
          case AggShape.TermsAggShape a -> a.field();
          case AggShape.DateHistogramAggShape a -> a.field();
          case AggShape.SumAggShape a -> a.field();
          case AggShape.AvgAggShape a -> a.field();
        };
    return validateField(field);
  }

  private String aggName(AggShape agg) {
    return switch (agg) {
      case AggShape.TermsAggShape a -> a.name();
      case AggShape.DateHistogramAggShape a -> a.name();
      case AggShape.SumAggShape a -> a.name();
      case AggShape.AvgAggShape a -> a.name();
    };
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

  private int resolveSize(java.util.Optional<Integer> requestedSize) {
    return requestedSize.map(s -> Math.min(s, MAX_SIZE)).orElse(DEFAULT_SIZE);
  }

  private static CalendarInterval toCalendarInterval(String interval) {
    return CalendarInterval.valueOf(
        interval.substring(0, 1).toUpperCase(Locale.ROOT)
            + interval.substring(1).toLowerCase(Locale.ROOT));
  }

  private org.opensearch.client.json.JsonData toFieldValueJson(Object value) {
    return org.opensearch.client.json.JsonData.of(value);
  }
}
