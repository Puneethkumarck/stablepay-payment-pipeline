package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.CustomerSummary;
import io.stablepay.api.infrastructure.web.dto.CustomerSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = AmountMapper.class)
public interface CustomerSummaryWebMapper {

  @Mapping(target = "id", expression = "java(summary.id().value().toString())")
  CustomerSummaryDto toDto(CustomerSummary summary);
}
