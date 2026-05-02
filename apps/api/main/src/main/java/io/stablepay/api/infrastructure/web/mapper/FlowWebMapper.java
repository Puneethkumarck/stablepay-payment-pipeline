package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.Flow;
import io.stablepay.api.infrastructure.web.dto.FlowDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = AmountMapper.class,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface FlowWebMapper {

  @Mapping(target = "id", expression = "java(flow.id().value().toString())")
  @Mapping(target = "customerId", expression = "java(flow.customerId().value().toString())")
  FlowDto toDto(Flow flow);
}
