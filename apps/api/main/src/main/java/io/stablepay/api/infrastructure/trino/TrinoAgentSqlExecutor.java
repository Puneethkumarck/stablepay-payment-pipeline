package io.stablepay.api.infrastructure.trino;

import io.stablepay.api.application.web.error.ErrorCodes;
import io.stablepay.api.domain.agent.SqlExecutionResult;
import io.stablepay.api.domain.port.AgentSqlExecutor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrinoAgentSqlExecutor implements AgentSqlExecutor {

  static final int QUERY_TIMEOUT_SECONDS = 30;

  @Qualifier("trinoDataSource")
  private final DataSource trinoDataSource;

  @Override
  public SqlExecutionResult executeQuery(String sanitizedSql, int appliedLimit) {
    var startNanos = System.nanoTime();
    var queryId = UUID.randomUUID().toString();
    try (var conn = trinoDataSource.getConnection()) {
      return executeWithConnection(conn, sanitizedSql, appliedLimit, queryId, startNanos);
    } catch (SQLException e) {
      log.warn("Agent SQL execution failed: queryId={}", queryId, e);
      return new SqlExecutionResult.Failed(
          ErrorCodes.SQL_EXECUTION_FAILED, "Query execution failed");
    }
  }

  private SqlExecutionResult executeWithConnection(
      Connection conn, String sql, int appliedLimit, String queryId, long startNanos)
      throws SQLException {
    try (var stmt = conn.prepareStatement(sql)) {
      stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      try (var rs = stmt.executeQuery()) {
        var meta = rs.getMetaData();
        var columnCount = meta.getColumnCount();
        var columns = new ArrayList<String>(columnCount);
        for (var i = 1; i <= columnCount; i++) {
          columns.add(meta.getColumnName(i));
        }
        var rows = new ArrayList<List<Object>>();
        while (rs.next() && rows.size() < appliedLimit) {
          var row = new ArrayList<Object>(columnCount);
          for (var i = 1; i <= columnCount; i++) {
            row.add(rs.getObject(i));
          }
          rows.add(List.copyOf(row));
        }
        var tookMs = (System.nanoTime() - startNanos) / 1_000_000;
        return new SqlExecutionResult.Success(
            List.copyOf(columns), List.copyOf(rows), rows.size(), queryId, tookMs);
      }
    }
  }
}
