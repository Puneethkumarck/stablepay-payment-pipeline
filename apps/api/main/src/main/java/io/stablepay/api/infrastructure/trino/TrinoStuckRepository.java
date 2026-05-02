package io.stablepay.api.infrastructure.trino;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.port.StuckRepository;
import io.stablepay.api.infrastructure.cursor.Base64PipeCursor;
import java.util.List;
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
public class TrinoStuckRepository implements StuckRepository {

  static final String CURSOR_ERROR_CODE = "STBLPAY-2102";

  static final String SQL_SEARCH_ADMIN_FIRST_PAGE =
      "SELECT transaction_id, transaction_reference, flow_type, internal_status, customer_id,"
          + " amount_micros, currency_code, last_event_at, stuck_millis"
          + " FROM iceberg.agg.agg_stuck_withdrawals"
          + " ORDER BY stuck_millis DESC, transaction_id DESC LIMIT :limit";

  static final String SQL_SEARCH_ADMIN_NEXT_PAGE =
      "SELECT transaction_id, transaction_reference, flow_type, internal_status, customer_id,"
          + " amount_micros, currency_code, last_event_at, stuck_millis"
          + " FROM iceberg.agg.agg_stuck_withdrawals"
          + " WHERE (stuck_millis, transaction_id) < (:cursorStuckMillis, :cursorTransactionId)"
          + " ORDER BY stuck_millis DESC, transaction_id DESC LIMIT :limit";

  static final RowMapper<StuckPayment> STUCK_ROW_MAPPER =
      (rs, rowNum) ->
          StuckPayment.builder()
              .id(TransactionId.of(UUID.fromString(rs.getString("transaction_id"))))
              .reference(rs.getString("transaction_reference"))
              .flowType(rs.getString("flow_type"))
              .internalStatus(rs.getString("internal_status"))
              .customerId(CustomerId.of(UUID.fromString(rs.getString("customer_id"))))
              .amount(
                  Money.fromMicros(
                      rs.getLong("amount_micros"),
                      CurrencyCode.getByCode(rs.getString("currency_code"))))
              .lastEventAt(rs.getTimestamp("last_event_at").toInstant())
              .stuckMillis(rs.getLong("stuck_millis"))
              .build();

  @Qualifier("trinoJdbcTemplate")
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public PaginatedResult<StuckPayment> searchAdmin(int pageSize, Optional<String> cursor) {
    Objects.requireNonNull(cursor, "cursor");
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1");
    }
    try {
      var rows =
          cursor.map(c -> queryNext(pageSize + 1, c)).orElseGet(() -> queryFirst(pageSize + 1));
      return paginate(rows, pageSize);
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  private List<StuckPayment> queryFirst(int limit) {
    var params = new MapSqlParameterSource().addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_FIRST_PAGE, params, STUCK_ROW_MAPPER);
  }

  private List<StuckPayment> queryNext(int limit, String cursor) {
    var decoded = decodeCursor(cursor);
    var params =
        new MapSqlParameterSource()
            .addValue("cursorStuckMillis", decoded.stuckMillis())
            .addValue("cursorTransactionId", decoded.transactionId())
            .addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_NEXT_PAGE, params, STUCK_ROW_MAPPER);
  }

  private static PaginatedResult<StuckPayment> paginate(List<StuckPayment> rows, int pageSize) {
    if (rows.size() <= pageSize) {
      return PaginatedResult.<StuckPayment>builder()
          .items(rows)
          .nextCursor(Optional.empty())
          .build();
    }
    var kept = rows.subList(0, pageSize);
    var last = kept.get(kept.size() - 1);
    var nextCursor = encodeCursor(last.stuckMillis(), last.id().value().toString());
    return PaginatedResult.<StuckPayment>builder()
        .items(kept)
        .nextCursor(Optional.of(nextCursor))
        .build();
  }

  static String encodeCursor(long stuckMillis, String transactionId) {
    return Base64PipeCursor.encode(stuckMillis, transactionId);
  }

  static TrinoStuckCursor decodeCursor(String cursor) {
    var part = Base64PipeCursor.decode(cursor, CURSOR_ERROR_CODE);
    return new TrinoStuckCursor(part.longPart(), part.stringPart());
  }
}
