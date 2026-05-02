package io.stablepay.api.infrastructure.web.mapper;

import static io.stablepay.api.domain.model.fixtures.DlqSummaryFixtures.SOME_DLQ_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.infrastructure.web.dto.DlqSummaryDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DlqSummaryWebMapperTest {

  private final DlqSummaryWebMapper mapper = Mappers.getMapper(DlqSummaryWebMapper.class);

  @Test
  void shouldMapDlqSummaryToDto() {
    // given
    var summary = SOME_DLQ_SUMMARY;
    var expected =
        DlqSummaryDto.builder()
            .countsByErrorClass(summary.countsByErrorClass())
            .totalCount(summary.totalCount())
            .build();

    // when
    var actual = mapper.toDto(summary);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
