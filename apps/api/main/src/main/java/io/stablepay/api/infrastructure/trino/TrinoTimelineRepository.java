package io.stablepay.api.infrastructure.trino;

import io.stablepay.api.domain.agent.TimelineEntry;
import io.stablepay.api.domain.port.TimelineRepository;
import java.util.List;
import java.util.Objects;
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
public class TrinoTimelineRepository implements TimelineRepository {

  static final String SQL_TIMELINE =
      "SELECT event_id, source, status, detail, event_time"
          + " FROM ("
          + "   SELECT event_id, 'transaction' AS source, internal_status AS status,"
          + "     flow_type AS detail, event_time"
          + "   FROM iceberg.analytics.v_transactions"
          + "   WHERE transaction_reference = :reference"
          + "   UNION ALL"
          + "   SELECT event_id, 'screening' AS source, screening_status AS status,"
          + "     screening_type AS detail, event_time"
          + "   FROM iceberg.analytics.v_screening"
          + "   WHERE transaction_reference = :reference"
          + " ) combined"
          + " ORDER BY event_time ASC";

  static final RowMapper<TimelineEntry> TIMELINE_ROW_MAPPER =
      (rs, rowNum) ->
          TimelineEntry.builder()
              .eventId(rs.getString("event_id"))
              .source(rs.getString("source"))
              .status(rs.getString("status"))
              .detail(rs.getString("detail"))
              .timestamp(rs.getTimestamp("event_time").toInstant())
              .build();

  @Qualifier("trinoJdbcTemplate")
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<TimelineEntry> findByReferenceAdmin(String reference) {
    Objects.requireNonNull(reference, "reference");
    var params = new MapSqlParameterSource().addValue("reference", reference);
    try {
      return jdbc.query(SQL_TIMELINE, params, TIMELINE_ROW_MAPPER);
    } catch (DataAccessException e) {
      throw new TrinoAdapterException(e);
    }
  }
}
