package io.stablepay.api.infrastructure.sse;

import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TransactionEventMapper {

  @Mapping(target = "customerId", expression = "java(tx.customerId().value().toString())")
  @Mapping(target = "status", source = "customerStatus")
  @Mapping(target = "amountMicros", expression = "java(tx.amount().toMicros())")
  @Mapping(target = "currencyCode", expression = "java(tx.amount().currency().name())")
  @Mapping(target = "sortKey", constant = "")
  TransactionEvent toEvent(Transaction tx);
}
