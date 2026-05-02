package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.FlowDto;
import io.stablepay.api.domain.model.Flow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = AmountMapper.class)
public interface FlowWebMapper {

  @Mapping(target = "id", expression = "java(flow.id().value().toString())")
  @Mapping(target = "customerId", expression = "java(flow.customerId().value().toString())")
  FlowDto toDto(Flow flow);
}
