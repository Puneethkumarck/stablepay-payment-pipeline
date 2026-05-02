package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.PaginatedResponse;
import io.stablepay.api.application.web.dto.TransactionDto;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = AmountMapper.class)
public interface TransactionWebMapper {

  @Mapping(target = "id", expression = "java(tx.id().value().toString())")
  @Mapping(target = "customerId", expression = "java(tx.customerId().value().toString())")
  @Mapping(target = "accountId", expression = "java(tx.accountId().value().toString())")
  @Mapping(target = "flowId", expression = "java(tx.flowId().value().toString())")
  TransactionDto toDto(Transaction tx);

  default PaginatedResponse<TransactionDto> toResponse(PaginatedResult<Transaction> result) {
    return PaginatedResponse.<TransactionDto>builder()
        .items(result.items().stream().map(this::toDto).toList())
        .nextCursor(result.nextCursor())
        .hasMore(result.nextCursor().isPresent())
        .build();
  }
}
