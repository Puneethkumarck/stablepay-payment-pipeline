package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.DlqSummaryDto;
import io.stablepay.api.domain.model.DlqSummary;
import org.mapstruct.Mapper;

@Mapper
public interface DlqSummaryWebMapper {

  DlqSummaryDto toDto(DlqSummary summary);
}
