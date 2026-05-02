package io.stablepay.api.infrastructure.trino;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.CustomerSummary;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.port.CustomerRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TrinoCustomerRepository implements CustomerRepository {

  static final String SQL_FIND_BY_ID =
      "SELECT customer_id, name, email, kyc, balance_micros, balance_currency_code,"
          + " total_sent_micros, total_sent_currency_code, txn_count, joined_at, risk_tier"
          + " FROM iceberg.analytics.v_customers"
          + " WHERE customer_id = :customerId LIMIT 1";

  static final RowMapper<CustomerSummary> CUSTOMER_ROW_MAPPER =
      (rs, rowNum) ->
          CustomerSummary.builder()
              .id(CustomerId.of(UUID.fromString(rs.getString("customer_id"))))
              .name(rs.getString("name"))
              .email(rs.getString("email"))
              .kyc(rs.getString("kyc"))
              .balance(
                  Money.fromMicros(
                      rs.getLong("balance_micros"),
                      CurrencyCode.getByCode(rs.getString("balance_currency_code"))))
              .totalSent(
                  Money.fromMicros(
                      rs.getLong("total_sent_micros"),
                      CurrencyCode.getByCode(rs.getString("total_sent_currency_code"))))
              .txnCount(rs.getInt("txn_count"))
              .joined(rs.getTimestamp("joined_at").toInstant())
              .risk(rs.getString("risk_tier"))
              .build();

  @Qualifier("trinoJdbcTemplate")
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<CustomerSummary> findById(CustomerId customerId) {
    return findByIdInternal(customerId);
  }

  @Override
  public Optional<CustomerSummary> findByIdAdmin(CustomerId id) {
    return findByIdInternal(id);
  }

  private Optional<CustomerSummary> findByIdInternal(CustomerId customerId) {
    Objects.requireNonNull(customerId, "customerId");
    var params = new MapSqlParameterSource().addValue("customerId", customerId.value().toString());
    try {
      return jdbc.query(SQL_FIND_BY_ID, params, CUSTOMER_ROW_MAPPER).stream().findFirst();
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }
}
