package io.stablepay.api.infrastructure.trino;

import static org.assertj.core.api.Assertions.assertThat;

import io.trino.sql.parser.SqlParser;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TrinoSqlParseTest {

  private static final Pattern NAMED_PARAM = Pattern.compile(":[a-zA-Z][a-zA-Z0-9_]*");

  private final SqlParser parser = new SqlParser();

  @Test
  void shouldParseAllFlowRepositoryQueries() {
    var queries =
        List.of(
            TrinoFlowRepository.SQL_FIND_BY_ID_FOR_CUSTOMER,
            TrinoFlowRepository.SQL_FIND_BY_ID_ADMIN,
            TrinoFlowRepository.SQL_SEARCH_BY_CUSTOMER_FIRST_PAGE,
            TrinoFlowRepository.SQL_SEARCH_BY_CUSTOMER_NEXT_PAGE,
            TrinoFlowRepository.SQL_SEARCH_ADMIN_FIRST_PAGE,
            TrinoFlowRepository.SQL_SEARCH_ADMIN_NEXT_PAGE);
    queries.forEach(this::assertParses);
    assertThat(queries).isNotEmpty();
  }

  @Test
  void shouldParseAllStuckRepositoryQueries() {
    var queries =
        List.of(
            TrinoStuckRepository.SQL_SEARCH_ADMIN_FIRST_PAGE,
            TrinoStuckRepository.SQL_SEARCH_ADMIN_NEXT_PAGE);
    queries.forEach(this::assertParses);
    assertThat(queries).isNotEmpty();
  }

  @Test
  void shouldParseAllCustomerRepositoryQueries() {
    var queries = List.of(TrinoCustomerRepository.SQL_FIND_BY_ID);
    queries.forEach(this::assertParses);
    assertThat(queries).isNotEmpty();
  }

  @Test
  void shouldParseAllDlqRepositoryQueries() {
    var queries =
        List.of(
            TrinoDlqRepository.SQL_FIND_BY_ID_ADMIN,
            TrinoDlqRepository.SQL_SEARCH_ADMIN_FIRST_PAGE,
            TrinoDlqRepository.SQL_SEARCH_ADMIN_NEXT_PAGE);
    queries.forEach(this::assertParses);
    assertThat(queries).isNotEmpty();
  }

  private void assertParses(String namedParameterSql) {
    var jdbcSql = NAMED_PARAM.matcher(namedParameterSql).replaceAll("?");
    var statement = parser.createStatement(jdbcSql);
    assertThat(statement).as("Trino parser should accept query: %s", jdbcSql).isNotNull();
  }
}
