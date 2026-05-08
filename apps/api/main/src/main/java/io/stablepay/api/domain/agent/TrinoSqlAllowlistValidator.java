package io.stablepay.api.domain.agent;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.DefaultTraversalVisitor;
import io.trino.sql.tree.Limit;
import io.trino.sql.tree.LongLiteral;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.Table;
import java.util.ArrayList;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrinoSqlAllowlistValidator {

  static final int DEFAULT_LIMIT = 1_000;
  static final int MAX_LIMIT = 10_000;

  private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\bLIMIT\\s+\\d+");

  private final AllowedTableRegistry allowedTableRegistry;
  private final SqlParser sqlParser = new SqlParser();

  public SqlValidationResult validate(String sql, int requestedLimit) {
    if (sql == null || sql.isBlank()) {
      return new SqlValidationResult.Invalid("SQL is empty", "STBLPAY-5001");
    }

    try {
      var statement = sqlParser.createStatement(sql.strip());

      if (!(statement instanceof Query query)) {
        return new SqlValidationResult.Invalid("Only SELECT queries allowed", "STBLPAY-5002");
      }

      if (query.getWith().isPresent() && query.getWith().get().isRecursive()) {
        return new SqlValidationResult.Invalid("WITH RECURSIVE not allowed", "STBLPAY-5003");
      }

      var cteNames = collectCteNames(query);

      var tableNames = new ArrayList<String>();
      new DefaultTraversalVisitor<Void>() {
        @Override
        protected Void visitTable(Table node, Void context) {
          tableNames.add(node.getName().toString());
          return null;
        }
      }.process(statement, null);

      for (var tableName : tableNames) {
        if (!cteNames.contains(tableName.toLowerCase(java.util.Locale.ROOT))
            && !allowedTableRegistry.isAllowed(tableName)) {
          return new SqlValidationResult.Invalid(
              "Table %s not in allowlist".formatted(tableName), "STBLPAY-5004");
        }
      }

      var appliedLimit = resolveLimit(query, requestedLimit);
      var sanitizedSql = rebuildWithLimit(sql.strip(), query, appliedLimit);

      return new SqlValidationResult.Valid(sanitizedSql, appliedLimit);
    } catch (ParsingException e) {
      return new SqlValidationResult.Invalid("SQL parse error", "STBLPAY-5001");
    }
  }

  private Set<String> collectCteNames(Query query) {
    if (query.getWith().isEmpty()) {
      return Set.of();
    }
    return query.getWith().get().getQueries().stream()
        .map(cte -> cte.getName().getValue().toLowerCase(java.util.Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }

  private int resolveLimit(Query query, int requestedLimit) {
    var capped = Math.min(Math.max(requestedLimit, 1), MAX_LIMIT);
    var existingLimit = extractLimit(query);
    if (existingLimit.isEmpty()) {
      return capped;
    }
    var sqlLimit = existingLimit.getAsLong();
    return (int) Math.min(sqlLimit, capped);
  }

  private OptionalLong extractLimit(Query query) {
    if (query.getLimit().isPresent()
        && query.getLimit().get() instanceof Limit queryLimit
        && queryLimit.getRowCount() instanceof LongLiteral longLiteral) {
      return OptionalLong.of(longLiteral.getParsedValue());
    }
    if (query.getQueryBody() instanceof QuerySpecification spec
        && spec.getLimit().isPresent()
        && spec.getLimit().get() instanceof Limit specLimit
        && specLimit.getRowCount() instanceof LongLiteral longLiteral) {
      return OptionalLong.of(longLiteral.getParsedValue());
    }
    return OptionalLong.empty();
  }

  private String rebuildWithLimit(String originalSql, Query query, int appliedLimit) {
    if (hasLimit(query)) {
      return replaceLastLimit(originalSql, appliedLimit);
    }
    return originalSql + " LIMIT " + appliedLimit;
  }

  private boolean hasLimit(Query query) {
    if (query.getLimit().isPresent()) {
      return true;
    }
    return query.getQueryBody() instanceof QuerySpecification spec && spec.getLimit().isPresent();
  }

  private String replaceLastLimit(String sql, int appliedLimit) {
    var matcher = LIMIT_PATTERN.matcher(sql);
    var lastStart = -1;
    var lastEnd = -1;
    while (matcher.find()) {
      lastStart = matcher.start();
      lastEnd = matcher.end();
    }
    if (lastStart == -1) {
      return sql;
    }
    return sql.substring(0, lastStart) + "LIMIT " + appliedLimit + sql.substring(lastEnd);
  }
}
