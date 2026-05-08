package io.stablepay.api.domain.agent;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.DefaultTraversalVisitor;
import io.trino.sql.tree.Limit;
import io.trino.sql.tree.LongLiteral;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.Statement;
import io.trino.sql.tree.Table;
import io.trino.sql.tree.With;
import java.util.ArrayList;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrinoSqlAllowlistValidator {

  static final int DEFAULT_LIMIT = 1_000;
  static final int MAX_LIMIT = 10_000;

  private final AllowedTableRegistry allowedTableRegistry;
  private final SqlParser sqlParser = new SqlParser();

  public SqlValidationResult validate(String sql) {
    if (sql == null || sql.isBlank()) {
      return new SqlValidationResult.Invalid("SQL is empty", "STBLPAY-5001");
    }

    Statement statement;
    try {
      statement = sqlParser.createStatement(sql.strip());
    } catch (ParsingException e) {
      return new SqlValidationResult.Invalid("SQL parse error", "STBLPAY-5001");
    }

    if (!(statement instanceof Query query)) {
      return new SqlValidationResult.Invalid("Only SELECT queries allowed", "STBLPAY-5002");
    }

    if (query.getWith().isPresent()) {
      var with = query.getWith().get();
      if (isRecursive(with)) {
        return new SqlValidationResult.Invalid("WITH RECURSIVE not allowed", "STBLPAY-5003");
      }
    }

    var tableNames = new ArrayList<String>();
    new DefaultTraversalVisitor<Void>() {
      @Override
      protected Void visitTable(Table node, Void context) {
        tableNames.add(node.getName().toString());
        return null;
      }
    }.process(statement, null);

    for (var tableName : tableNames) {
      if (!allowedTableRegistry.isAllowed(tableName)) {
        return new SqlValidationResult.Invalid(
            "Table %s not in allowlist".formatted(tableName), "STBLPAY-5004");
      }
    }

    var appliedLimit = resolveLimit(query);
    var sanitizedSql = rebuildWithLimit(sql.strip(), query, appliedLimit);

    return new SqlValidationResult.Valid(sanitizedSql, appliedLimit);
  }

  private boolean isRecursive(With with) {
    return with.isRecursive();
  }

  private int resolveLimit(Query query) {
    var existingLimit = extractLimit(query);
    if (existingLimit.isEmpty()) {
      return DEFAULT_LIMIT;
    }
    var userLimit = existingLimit.getAsLong();
    return (int) Math.min(userLimit, MAX_LIMIT);
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
      return replaceLimit(originalSql, appliedLimit);
    }
    return originalSql + " LIMIT " + appliedLimit;
  }

  private boolean hasLimit(Query query) {
    if (query.getLimit().isPresent()) {
      return true;
    }
    return query.getQueryBody() instanceof QuerySpecification spec && spec.getLimit().isPresent();
  }

  private String replaceLimit(String sql, int appliedLimit) {
    return sql.replaceAll("(?i)\\bLIMIT\\s+\\d+", "LIMIT " + appliedLimit);
  }
}
