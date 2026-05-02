package io.stablepay.api.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.CachedResponse;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresIdempotencyRepositoryTest {

  @Mock ResultSet rs;

  @Test
  void cachedResponseRowMapper_buildsCachedResponse_whenAllColumnsPresent() throws Exception {
    // given
    var expiresAt = Instant.parse("2026-05-01T11:00:00Z");
    var bodyBytes = new byte[] {1, 2, 3};
    given(rs.getInt("response_status")).willReturn(200);
    given(rs.getBytes("response_body")).willReturn(bodyBytes);
    given(rs.getTimestamp("expires_at")).willReturn(Timestamp.from(expiresAt));
    var expected = new CachedResponse(200, new byte[] {1, 2, 3}, expiresAt);

    // when
    var actual = PostgresIdempotencyRepository.CACHED_RESPONSE_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void cachedResponseRowMapper_buildsCachedResponseWithEmptyBody_whenBodyIsNull() throws Exception {
    // given
    var expiresAt = Instant.parse("2026-05-01T11:00:00Z");
    given(rs.getInt("response_status")).willReturn(204);
    given(rs.getBytes("response_body")).willReturn(null);
    given(rs.getTimestamp("expires_at")).willReturn(Timestamp.from(expiresAt));
    var expected = new CachedResponse(204, new byte[0], expiresAt);

    // when
    var actual = PostgresIdempotencyRepository.CACHED_RESPONSE_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
