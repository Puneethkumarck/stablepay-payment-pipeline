package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
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
    if (fieldResult.isPresent()) {
      return fieldResult.get();
    }

    var sortResult = validateSortFields(request.sort());
    if (sortResult.isPresent()) {
      return sortResult.get();
    }

    var aggResult = validateAggs(request.aggs());
    if (aggResult.isPresent()) {
      return aggResult.get();
    }

    var resolvedSize = resolveSize(request.size());
    return new DslValidationResult.Valid(request, resolvedSize);
  }

  private Optional<DslValidationResult.Invalid> validateFields(QueryShape shape) {
    return switch (shape) {
      case QueryShape.TermShape term -> validateField(term.field());
      case QueryShape.RangeShape range -> validateField(range.field());
      case QueryShape.MatchPhraseShape matchPhrase -> validateField(matchPhrase.field());
      case QueryShape.ExistsShape exists -> validateField(exists.field());
      case QueryShape.TermsShape terms -> validateField(terms.field());
      case QueryShape.BoolShape bool -> validateBoolFields(bool);
    };
  }

  private Optional<DslValidationResult.Invalid> validateBoolFields(QueryShape.BoolShape bool) {
    return Stream.of(bool.must(), bool.filter(), bool.mustNot())
        .flatMap(List::stream)
        .map(this::validateFields)
        .flatMap(Optional::stream)
        .findFirst();
  }

  private Optional<DslValidationResult.Invalid> validateField(String field) {
    if (!registry.isFieldAllowed(field)) {
      return Optional.of(
          new DslValidationResult.Invalid(
              "Field not in allowlist: %s".formatted(field), "STBLPAY-6002"));
    }
    return Optional.empty();
  }

  private Optional<DslValidationResult.Invalid> validateSortFields(Optional<List<String>> sort) {
    return sort.stream()
        .flatMap(List::stream)
        .filter(field -> !registry.isFieldAllowed(field))
        .findFirst()
        .map(
            field ->
                new DslValidationResult.Invalid(
                    "Sort field not in allowlist: %s".formatted(field), "STBLPAY-6005"));
  }

  private Optional<DslValidationResult.Invalid> validateAggs(Optional<List<AggShape>> aggs) {
    if (aggs.isEmpty()) {
      return Optional.empty();
    }
    return aggs.get().stream().map(this::validateAgg).flatMap(Optional::stream).findFirst();
  }

  private Optional<DslValidationResult.Invalid> validateAgg(AggShape agg) {
    var fieldResult = validateAggField(agg);
    if (fieldResult.isPresent()) {
      return fieldResult;
    }
    return validateAggProperties(agg);
  }

  private Optional<DslValidationResult.Invalid> validateAggField(AggShape agg) {
    var field =
        switch (agg) {
          case AggShape.TermsAggShape a -> a.field();
          case AggShape.DateHistogramAggShape a -> a.field();
          case AggShape.SumAggShape a -> a.field();
          case AggShape.AvgAggShape a -> a.field();
        };
    return validateField(field);
  }

  private Optional<DslValidationResult.Invalid> validateAggProperties(AggShape agg) {
    return switch (agg) {
      case AggShape.DateHistogramAggShape a -> {
        if (!registry.isCalendarIntervalAllowed(a.calendarInterval())) {
          yield Optional.of(
              new DslValidationResult.Invalid(
                  "Calendar interval not allowed: %s".formatted(a.calendarInterval()),
                  "STBLPAY-6003"));
        }
        yield Optional.empty();
      }
      default -> Optional.empty();
    };
  }

  private int resolveSize(Optional<Integer> requestedSize) {
    return requestedSize.map(s -> Math.max(1, Math.min(s, MAX_SIZE))).orElse(DEFAULT_SIZE);
  }
}
