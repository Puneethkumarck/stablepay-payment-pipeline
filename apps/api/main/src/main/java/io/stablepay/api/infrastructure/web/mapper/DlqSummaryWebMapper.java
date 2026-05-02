package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.DlqSummary;
import io.stablepay.api.infrastructure.web.dto.DlqSummaryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DlqSummaryWebMapper {

  DlqSummaryDto toDto(DlqSummary summary);
}
