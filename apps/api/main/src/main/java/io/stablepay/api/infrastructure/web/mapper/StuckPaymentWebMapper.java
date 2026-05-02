package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import io.stablepay.api.infrastructure.web.dto.PaginatedResponse;
import io.stablepay.api.infrastructure.web.dto.StuckPaymentDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = AmountMapper.class,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
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
