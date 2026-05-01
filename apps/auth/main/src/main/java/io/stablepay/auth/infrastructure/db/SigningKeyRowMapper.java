package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.SigningKey;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SigningKeyRowMapper {

  SigningKey toDomain(SigningKeyRow row);
}
