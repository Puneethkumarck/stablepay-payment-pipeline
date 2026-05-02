package io.stablepay.api.infrastructure.trino;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.Flow;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.port.FlowRepository;
import io.stablepay.api.infrastructure.cursor.Base64PipeCursor;
import java.sql.Timestamp;
import java.time.Instant;
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

@Repository
@Slf4j
public class TrinoFlowRepository implements FlowRepository {

  static final String CURSOR_ERROR_CODE = "STBLPAY-2102";

  static final String SQL_FIND_BY_ID_FOR_CUSTOMER =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " WHERE flow_id = :flowId AND customer_id = :customerId LIMIT 1";

  static final String SQL_FIND_BY_ID_ADMIN =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " WHERE flow_id = :flowId LIMIT 1";

  static final String SQL_SEARCH_BY_CUSTOMER_FIRST_PAGE =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " WHERE customer_id = :customerId"
          + " ORDER BY created_at DESC, flow_id DESC LIMIT :limit";

  static final String SQL_SEARCH_BY_CUSTOMER_NEXT_PAGE =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " WHERE customer_id = :customerId"
          + " AND (created_at, flow_id) < (CAST(:cursorCreatedAt AS TIMESTAMP(6) WITH TIME ZONE),"
          + " :cursorFlowId)"
          + " ORDER BY created_at DESC, flow_id DESC LIMIT :limit";

  static final String SQL_SEARCH_ADMIN_FIRST_PAGE =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " ORDER BY created_at DESC, flow_id DESC LIMIT :limit";

  static final String SQL_SEARCH_ADMIN_NEXT_PAGE =
      "SELECT flow_id, flow_type, status, customer_id, total_amount_micros, total_currency_code,"
          + " leg_count, created_at, updated_at, completed_at"
          + " FROM iceberg.analytics.v_flows"
          + " WHERE (created_at, flow_id) < (CAST(:cursorCreatedAt AS TIMESTAMP(6) WITH TIME ZONE),"
          + " :cursorFlowId)"
          + " ORDER BY created_at DESC, flow_id DESC LIMIT :limit";

  static final RowMapper<Flow> FLOW_ROW_MAPPER =
      (rs, rowNum) ->
          Flow.builder()
              .id(FlowId.of(UUID.fromString(rs.getString("flow_id"))))
              .flowType(rs.getString("flow_type"))
              .status(rs.getString("status"))
              .customerId(CustomerId.of(UUID.fromString(rs.getString("customer_id"))))
              .totalAmount(
                  Money.fromMicros(
                      rs.getLong("total_amount_micros"),
                      CurrencyCode.getByCode(rs.getString("total_currency_code"))))
              .legCount(rs.getInt("leg_count"))
              .createdAt(rs.getTimestamp("created_at").toInstant())
              .updatedAt(rs.getTimestamp("updated_at").toInstant())
              .completedAt(
                  Optional.ofNullable(rs.getTimestamp("completed_at")).map(Timestamp::toInstant))
              .build();

  private final NamedParameterJdbcTemplate jdbc;

  public TrinoFlowRepository(@Qualifier("trinoJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Flow> findById(FlowId id, CustomerId customerId) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(customerId, "customerId");
    var params =
        new MapSqlParameterSource()
            .addValue("flowId", id.value().toString())
            .addValue("customerId", customerId.value().toString());
    try {
      return jdbc.query(SQL_FIND_BY_ID_FOR_CUSTOMER, params, FLOW_ROW_MAPPER).stream().findFirst();
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  @Override
  public Optional<Flow> findByIdAdmin(FlowId id) {
    Objects.requireNonNull(id, "id");
    var params = new MapSqlParameterSource().addValue("flowId", id.value().toString());
    try {
      return jdbc.query(SQL_FIND_BY_ID_ADMIN, params, FLOW_ROW_MAPPER).stream().findFirst();
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  @Override
  public PaginatedResult<Flow> searchByCustomer(
      CustomerId customerId, int pageSize, Optional<String> cursor) {
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(cursor, "cursor");
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1");
    }
    try {
      var rows =
          cursor
              .map(c -> queryByCustomerNext(customerId, pageSize + 1, c))
              .orElseGet(() -> queryByCustomerFirst(customerId, pageSize + 1));
      return paginate(rows, pageSize);
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  @Override
  public PaginatedResult<Flow> searchAdmin(int pageSize, Optional<String> cursor) {
    Objects.requireNonNull(cursor, "cursor");
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1");
    }
    try {
      var rows =
          cursor
              .map(c -> queryAdminNext(pageSize + 1, c))
              .orElseGet(() -> queryAdminFirst(pageSize + 1));
      return paginate(rows, pageSize);
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  private List<Flow> queryByCustomerFirst(CustomerId customerId, int limit) {
    var params =
        new MapSqlParameterSource()
            .addValue("customerId", customerId.value().toString())
            .addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_BY_CUSTOMER_FIRST_PAGE, params, FLOW_ROW_MAPPER);
  }

  private List<Flow> queryByCustomerNext(CustomerId customerId, int limit, String cursor) {
    var decoded = decodeCursor(cursor);
    var params =
        new MapSqlParameterSource()
            .addValue("customerId", customerId.value().toString())
            .addValue("cursorCreatedAt", Timestamp.from(decoded.createdAt()))
            .addValue("cursorFlowId", decoded.id())
            .addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_BY_CUSTOMER_NEXT_PAGE, params, FLOW_ROW_MAPPER);
  }

  private List<Flow> queryAdminFirst(int limit) {
    var params = new MapSqlParameterSource().addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_FIRST_PAGE, params, FLOW_ROW_MAPPER);
  }

  private List<Flow> queryAdminNext(int limit, String cursor) {
    var decoded = decodeCursor(cursor);
    var params =
        new MapSqlParameterSource()
            .addValue("cursorCreatedAt", Timestamp.from(decoded.createdAt()))
            .addValue("cursorFlowId", decoded.id())
            .addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_NEXT_PAGE, params, FLOW_ROW_MAPPER);
  }

  private static PaginatedResult<Flow> paginate(List<Flow> rows, int pageSize) {
    if (rows.size() <= pageSize) {
      return PaginatedResult.<Flow>builder().items(rows).nextCursor(Optional.empty()).build();
    }
    var kept = rows.subList(0, pageSize);
    var last = kept.get(kept.size() - 1);
    var nextCursor = encodeCursor(last.createdAt().toEpochMilli(), last.id().value().toString());
    return PaginatedResult.<Flow>builder().items(kept).nextCursor(Optional.of(nextCursor)).build();
  }

  static String encodeCursor(long createdAtEpochMillis, String flowId) {
    return Base64PipeCursor.encode(createdAtEpochMillis, flowId);
  }

  static DecodedCursor decodeCursor(String cursor) {
    var part = Base64PipeCursor.decode(cursor, CURSOR_ERROR_CODE);
    return new DecodedCursor(Instant.ofEpochMilli(part.longPart()), part.stringPart());
  }

  record DecodedCursor(Instant createdAt, String id) {}
}
