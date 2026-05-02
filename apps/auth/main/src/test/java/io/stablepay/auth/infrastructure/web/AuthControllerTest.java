package io.stablepay.auth.infrastructure.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.auth.application.AuthService;
import io.stablepay.auth.application.AuthService.LoginOutcome;
import io.stablepay.auth.client.ApiError;
import io.stablepay.auth.client.LoginRequest;
import io.stablepay.auth.client.RefreshRequest;
import io.stablepay.auth.client.TokenResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");
  private static final String SOME_EMAIL = "alice@example.com";
  private static final String SOME_PASSWORD = "Sup3rSecret!";
  private static final String SOME_ACCESS_TOKEN = "access-token-xyz";
  private static final String SOME_REFRESH_TOKEN = "refresh-token-abc";
  private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

  @Mock private AuthService authService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    var controller = new AuthController(authService, clock);
    var advice = new GlobalExceptionHandler(clock);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build();
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Test
  void shouldReturn200AndTokenResponseOnLoginSuccess() throws Exception {
    // given
    var expected = new TokenResponse(SOME_ACCESS_TOKEN, SOME_REFRESH_TOKEN, ACCESS_TTL.toSeconds());
    given(authService.login(SOME_EMAIL, SOME_PASSWORD, FIXED_NOW))
        .willReturn(new LoginOutcome.Success(SOME_ACCESS_TOKEN, SOME_REFRESH_TOKEN, ACCESS_TTL));
    var requestBody = objectMapper.writeValueAsString(new LoginRequest(SOME_EMAIL, SOME_PASSWORD));

    // when
    var responseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var actual = objectMapper.readValue(responseBody, TokenResponse.class);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturn401AndApiErrorOnInvalidLoginCredentials() throws Exception {
    // given
    var expected = new ApiError("STBLPAY-1001", "Invalid credentials", FIXED_NOW);
    given(authService.login(SOME_EMAIL, SOME_PASSWORD, FIXED_NOW))
        .willReturn(new LoginOutcome.InvalidCredentials());
    var requestBody = objectMapper.writeValueAsString(new LoginRequest(SOME_EMAIL, SOME_PASSWORD));

    // when
    var responseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var actual = objectMapper.readValue(responseBody, ApiError.class);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturn200AndTokenResponseOnRefreshSuccess() throws Exception {
    // given
    var newAccess = "new-access-token";
    var newRefresh = "new-refresh-token";
    var expected = new TokenResponse(newAccess, newRefresh, ACCESS_TTL.toSeconds());
    given(authService.refresh(SOME_REFRESH_TOKEN, FIXED_NOW))
        .willReturn(new LoginOutcome.Success(newAccess, newRefresh, ACCESS_TTL));
    var requestBody = objectMapper.writeValueAsString(new RefreshRequest(SOME_REFRESH_TOKEN));

    // when
    var responseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var actual = objectMapper.readValue(responseBody, TokenResponse.class);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturn401AndApiErrorOnInvalidRefreshToken() throws Exception {
    // given
    var expected = new ApiError("STBLPAY-1002", "Invalid credentials", FIXED_NOW);
    given(authService.refresh(SOME_REFRESH_TOKEN, FIXED_NOW))
        .willReturn(new LoginOutcome.InvalidCredentials());
    var requestBody = objectMapper.writeValueAsString(new RefreshRequest(SOME_REFRESH_TOKEN));

    // when
    var responseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var actual = objectMapper.readValue(responseBody, ApiError.class);

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldReturn204AndDelegateToAuthServiceOnLogout() throws Exception {
    // given
    var requestBody = objectMapper.writeValueAsString(new RefreshRequest(SOME_REFRESH_TOKEN));

    // when
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(header().doesNotExist("Content-Type"));

    // then
    then(authService).should().logout(SOME_REFRESH_TOKEN, FIXED_NOW);
  }
}
