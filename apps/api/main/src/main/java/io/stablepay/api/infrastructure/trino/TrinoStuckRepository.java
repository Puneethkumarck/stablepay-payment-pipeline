package io.stablepay.api.infrastructure.trino;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.port.StuckRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Trino-backed read adapter for {@link StuckPayment}. Reads from the {@code
 * iceberg.agg.agg_stuck_withdrawals} aggregate maintained by the Flink correlator job.
 *
 * <p>Expected columns: {@code transaction_id, transaction_reference, flow_type, internal_status,
 * customer_id, amount_micros, currency_code, last_event_at, stuck_millis}.
 *
 * <p>Pagination uses an opaque url-safe base64 cursor over {@code <stuck_millis>|<transaction_id>}
 * so the keyset predicate {@code (stuck_millis, transaction_id) < (cursor)} is stable across page
 * boundaries.
 */
@Repository
@Slf4j
public class TrinoStuckRepository implements StuckRepository {

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

  private final NamedParameterJdbcTemplate jdbc;

  public TrinoStuckRepository(@Qualifier("trinoJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

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
    var raw = stuckMillis + "|" + transactionId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  static DecodedCursor decodeCursor(String cursor) {
    Objects.requireNonNull(cursor, "cursor");
    try {
      var decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      var pipe = decoded.indexOf('|');
      if (pipe <= 0 || pipe == decoded.length() - 1) {
        throw new IllegalArgumentException("STBLPAY-2102 malformed cursor payload");
      }
      var stuckMillis = Long.parseLong(decoded.substring(0, pipe));
      var transactionId = decoded.substring(pipe + 1);
      return new DecodedCursor(stuckMillis, transactionId);
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("STBLPAY-2102")) {
        throw e;
      }
      throw new IllegalArgumentException("STBLPAY-2102 invalid cursor", e);
    }
  }

  record DecodedCursor(long stuckMillis, String transactionId) {}
}
