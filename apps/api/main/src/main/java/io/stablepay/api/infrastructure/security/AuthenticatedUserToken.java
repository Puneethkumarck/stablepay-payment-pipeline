package io.stablepay.api.infrastructure.security;

import io.stablepay.api.application.security.AuthenticatedUser;
import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedUserToken extends AbstractAuthenticationToken {

  private final Jwt jwt;
  private final AuthenticatedUser principal;

  public AuthenticatedUserToken(
      Jwt jwt, AuthenticatedUser principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.jwt = Objects.requireNonNull(jwt, "jwt");
    this.principal = Objects.requireNonNull(principal, "principal");
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return jwt;
  }

  @Override
  public AuthenticatedUser getPrincipal() {
    return principal;
  }

  public Jwt getToken() {
    return jwt;
  }

  @Override
  public String getName() {
    return principal.email();
  }
}
