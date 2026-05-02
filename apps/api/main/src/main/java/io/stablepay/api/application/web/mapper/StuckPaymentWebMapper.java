package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.dto.StuckPaymentDto;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = AmountMapper.class)
public interface StuckPaymentWebMapper {

  @Mapping(target = "id", expression = "java(payment.id().value().toString())")
  @Mapping(target = "customerId", expression = "java(payment.customerId().value().toString())")
  StuckPaymentDto toDto(StuckPayment payment);

  default PaginatedResponse<StuckPaymentDto> toResponse(PaginatedResult<StuckPayment> result) {
    return PaginatedResponse.<StuckPaymentDto>builder()
        .items(result.items().stream().map(this::toDto).toList())
        .nextCursor(result.nextCursor())
        .hasMore(result.nextCursor().isPresent())
        .build();
  }
}
