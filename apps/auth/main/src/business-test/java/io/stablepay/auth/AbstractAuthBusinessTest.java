package io.stablepay.auth;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import io.stablepay.auth.client.ApiError;
import io.stablepay.auth.client.LoginRequest;
import io.stablepay.auth.client.RefreshRequest;
import io.stablepay.auth.client.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@AutoConfigureTestRestTemplate
public abstract class AbstractAuthBusinessTest {

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("stablepay_auth")
          .withUsername("stablepay")
          .withPassword("stablepay");

  static {
    POSTGRES.start();
  }

  @Autowired protected TestRestTemplate restTemplate;

  @DynamicPropertySource
  static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  protected ResponseEntity<TokenResponse> postLogin(String email, String password) {
    return restTemplate.exchange(
        "/api/v1/auth/login",
        POST,
        jsonEntity(new LoginRequest(email, password)),
        TokenResponse.class);
  }

  protected ResponseEntity<ApiError> postLoginExpectingError(String email, String password) {
    return restTemplate.exchange(
        "/api/v1/auth/login", POST, jsonEntity(new LoginRequest(email, password)), ApiError.class);
  }

  protected ResponseEntity<TokenResponse> postRefresh(String refreshToken) {
    return restTemplate.exchange(
        "/api/v1/auth/refresh",
        POST,
        jsonEntity(new RefreshRequest(refreshToken)),
        TokenResponse.class);
  }

  protected ResponseEntity<ApiError> postRefreshExpectingError(String refreshToken) {
    return restTemplate.exchange(
        "/api/v1/auth/refresh", POST, jsonEntity(new RefreshRequest(refreshToken)), ApiError.class);
  }

  protected ResponseEntity<Void> postLogout(String refreshToken) {
    return restTemplate.exchange(
        "/api/v1/auth/logout", POST, jsonEntity(new RefreshRequest(refreshToken)), Void.class);
  }

  protected static <T> HttpEntity<T> jsonEntity(T body) {
    var headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }
}
