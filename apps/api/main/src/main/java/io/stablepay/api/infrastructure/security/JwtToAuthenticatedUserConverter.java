package io.stablepay.api.infrastructure.security;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtToAuthenticatedUserConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String CLAIM_ROLES = "roles";
  private static final String CLAIM_CUSTOMER_ID = "customer_id";
  private static final String CLAIM_EMAIL = "email";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    var roleClaims = Optional.ofNullable(jwt.getClaimAsStringList(CLAIM_ROLES)).orElse(List.of());
    var authorities =
        roleClaims.stream().map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)).toList();
    var customerId =
        Optional.ofNullable(jwt.getClaimAsString(CLAIM_CUSTOMER_ID))
            .map(value -> CustomerId.of(UUID.fromString(value)));
    var roles = roleClaims.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());
    var user =
        AuthenticatedUser.builder()
            .userId(UserId.of(UUID.fromString(jwt.getSubject())))
            .customerId(customerId)
            .roles(roles)
            .email(jwt.getClaimAsString(CLAIM_EMAIL))
            .build();
    return new AuthenticatedUserToken(jwt, user, authorities);
  }
}
