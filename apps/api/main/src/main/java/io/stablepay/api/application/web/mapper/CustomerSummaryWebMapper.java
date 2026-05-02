package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.CustomerSummaryDto;
import io.stablepay.api.domain.model.CustomerSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = AmountMapper.class)
public interface CustomerSummaryWebMapper {

  @Mapping(target = "id", expression = "java(summary.id().value().toString())")
  CustomerSummaryDto toDto(CustomerSummary summary);
}
