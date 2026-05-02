package io.stablepay.api.infrastructure.web.mapper;

import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.infrastructure.web.dto.DlqEventDto;
import io.stablepay.api.infrastructure.web.dto.PaginatedResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DlqEventWebMapperTest {

  private final DlqEventWebMapper mapper = Mappers.getMapper(DlqEventWebMapper.class);

  @Test
  void shouldMapDlqEventToDto() {
    // given
    var dlqEvent = SOME_DLQ_EVENT;
    var expected =
        DlqEventDto.builder()
            .id(dlqEvent.id().value().toString())
            .errorClass(dlqEvent.errorClass())
            .sourceTopic(dlqEvent.sourceTopic())
            .sourcePartition(dlqEvent.sourcePartition())
            .sourceOffset(dlqEvent.sourceOffset())
            .errorMessage(dlqEvent.errorMessage())
            .failedAt(dlqEvent.failedAt())
            .retryCount(dlqEvent.retryCount())
            .sinkType(dlqEvent.sinkType())
            .watermarkAt(dlqEvent.watermarkAt())
            .originalPayloadJson(dlqEvent.originalPayloadJson())
            .build();

    // when
    var actual = mapper.toDto(dlqEvent);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldMapPaginatedResultToResponse() {
    // given
    var paginatedResult =
        PaginatedResult.<io.stablepay.api.domain.model.DlqEvent>builder()
            .items(List.of(SOME_DLQ_EVENT))
            .nextCursor(Optional.of("dlq-cursor-1"))
            .build();
    var expectedItem =
        DlqEventDto.builder()
            .id(SOME_DLQ_EVENT.id().value().toString())
            .errorClass(SOME_DLQ_EVENT.errorClass())
            .sourceTopic(SOME_DLQ_EVENT.sourceTopic())
            .sourcePartition(SOME_DLQ_EVENT.sourcePartition())
            .sourceOffset(SOME_DLQ_EVENT.sourceOffset())
            .errorMessage(SOME_DLQ_EVENT.errorMessage())
            .failedAt(SOME_DLQ_EVENT.failedAt())
            .retryCount(SOME_DLQ_EVENT.retryCount())
            .sinkType(SOME_DLQ_EVENT.sinkType())
            .watermarkAt(SOME_DLQ_EVENT.watermarkAt())
            .originalPayloadJson(SOME_DLQ_EVENT.originalPayloadJson())
            .build();
    var expected =
        PaginatedResponse.<DlqEventDto>builder()
            .items(List.of(expectedItem))
            .nextCursor(Optional.of("dlq-cursor-1"))
            .hasMore(true)
            .build();

    // when
    var actual = mapper.toResponse(paginatedResult);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
