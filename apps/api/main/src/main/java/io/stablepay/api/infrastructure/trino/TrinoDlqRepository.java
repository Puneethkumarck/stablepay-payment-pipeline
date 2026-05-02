package io.stablepay.api.infrastructure.trino;

import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.port.DlqRepository;
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
public class TrinoDlqRepository implements DlqRepository {

  static final String CURSOR_ERROR_CODE = "STBLPAY-2102";

  static final String SQL_FIND_BY_ID_ADMIN =
      "SELECT dlq_id, error_class, source_topic, source_partition, source_offset, error_message,"
          + " failed_at, retry_count, sink_type, watermark_at, original_payload_json"
          + " FROM iceberg.dlq.dlq_events"
          + " WHERE dlq_id = :dlqId LIMIT 1";

  static final String SQL_SEARCH_ADMIN_FIRST_PAGE =
      "SELECT dlq_id, error_class, source_topic, source_partition, source_offset, error_message,"
          + " failed_at, retry_count, sink_type, watermark_at, original_payload_json"
          + " FROM iceberg.dlq.dlq_events"
          + " ORDER BY failed_at DESC, dlq_id DESC LIMIT :limit";

  static final String SQL_SEARCH_ADMIN_NEXT_PAGE =
      "SELECT dlq_id, error_class, source_topic, source_partition, source_offset, error_message,"
          + " failed_at, retry_count, sink_type, watermark_at, original_payload_json"
          + " FROM iceberg.dlq.dlq_events"
          + " WHERE (failed_at, dlq_id) < (CAST(:cursorFailedAt AS TIMESTAMP(6) WITH TIME ZONE),"
          + " :cursorDlqId)"
          + " ORDER BY failed_at DESC, dlq_id DESC LIMIT :limit";

  static final RowMapper<DlqEvent> DLQ_ROW_MAPPER =
      (rs, rowNum) ->
          DlqEvent.builder()
              .id(DlqId.of(UUID.fromString(rs.getString("dlq_id"))))
              .errorClass(rs.getString("error_class"))
              .sourceTopic(rs.getString("source_topic"))
              .sourcePartition(rs.getInt("source_partition"))
              .sourceOffset(rs.getLong("source_offset"))
              .errorMessage(rs.getString("error_message"))
              .failedAt(rs.getTimestamp("failed_at").toInstant())
              .retryCount(rs.getInt("retry_count"))
              .sinkType(Optional.ofNullable(rs.getString("sink_type")))
              .watermarkAt(
                  Optional.ofNullable(rs.getTimestamp("watermark_at")).map(Timestamp::toInstant))
              .originalPayloadJson(Optional.ofNullable(rs.getString("original_payload_json")))
              .build();

  private final NamedParameterJdbcTemplate jdbc;

  public TrinoDlqRepository(@Qualifier("trinoJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<DlqEvent> findByIdAdmin(DlqId id) {
    Objects.requireNonNull(id, "id");
    var params = new MapSqlParameterSource().addValue("dlqId", id.value().toString());
    try {
      return jdbc.query(SQL_FIND_BY_ID_ADMIN, params, DLQ_ROW_MAPPER).stream().findFirst();
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }

  @Override
  public PaginatedResult<DlqEvent> searchAdmin(int pageSize, Optional<String> cursor) {
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

  private List<DlqEvent> queryFirst(int limit) {
    var params = new MapSqlParameterSource().addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_FIRST_PAGE, params, DLQ_ROW_MAPPER);
  }

  private List<DlqEvent> queryNext(int limit, String cursor) {
    var decoded = decodeCursor(cursor);
    var params =
        new MapSqlParameterSource()
            .addValue("cursorFailedAt", Timestamp.from(decoded.failedAt()))
            .addValue("cursorDlqId", decoded.dlqId())
            .addValue("limit", limit);
    return jdbc.query(SQL_SEARCH_ADMIN_NEXT_PAGE, params, DLQ_ROW_MAPPER);
  }

  private static PaginatedResult<DlqEvent> paginate(List<DlqEvent> rows, int pageSize) {
    if (rows.size() <= pageSize) {
      return PaginatedResult.<DlqEvent>builder().items(rows).nextCursor(Optional.empty()).build();
    }
    var kept = rows.subList(0, pageSize);
    var last = kept.get(kept.size() - 1);
    var nextCursor = encodeCursor(last.failedAt().toEpochMilli(), last.id().value().toString());
    return PaginatedResult.<DlqEvent>builder()
        .items(kept)
        .nextCursor(Optional.of(nextCursor))
        .build();
  }

  static String encodeCursor(long failedAtEpochMillis, String dlqId) {
    return Base64PipeCursor.encode(failedAtEpochMillis, dlqId);
  }

  static DecodedCursor decodeCursor(String cursor) {
    var part = Base64PipeCursor.decode(cursor, CURSOR_ERROR_CODE);
    return new DecodedCursor(Instant.ofEpochMilli(part.longPart()), part.stringPart());
  }

  record DecodedCursor(Instant failedAt, String dlqId) {}
}
