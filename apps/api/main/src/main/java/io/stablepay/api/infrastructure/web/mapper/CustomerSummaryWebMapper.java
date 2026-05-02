package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.CustomerSummary;
import io.stablepay.api.infrastructure.web.dto.CustomerSummaryDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = AmountMapper.class,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CustomerSummaryWebMapper {

  @Mapping(target = "id", expression = "java(summary.id().value().toString())")
  CustomerSummaryDto toDto(CustomerSummary summary);
}
