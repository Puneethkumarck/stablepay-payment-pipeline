package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.TransactionEventDto;
import io.stablepay.api.domain.model.TransactionEvent;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionEventWebMapper {

  TransactionEventDto toDto(TransactionEvent event);
}
