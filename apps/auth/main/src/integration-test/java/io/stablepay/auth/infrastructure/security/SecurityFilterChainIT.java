package io.stablepay.auth.infrastructure.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.auth.application.AuthService;
import io.stablepay.auth.application.AuthService.LoginOutcome;
import io.stablepay.auth.application.SigningKeyManager;
import io.stablepay.auth.client.LoginRequest;
import io.stablepay.auth.infrastructure.ratelimit.LoginRateLimitFilter;
import io.stablepay.auth.infrastructure.web.AuthController;
import io.stablepay.auth.infrastructure.web.GlobalExceptionHandler;
import io.stablepay.auth.infrastructure.web.JwksController;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, JwksController.class})
@Import({
  SecurityConfig.class,
  LoginRateLimitFilter.class,
  GlobalExceptionHandler.class,
  SecurityFilterChainIT.FixedClockConfig.class
})
class SecurityFilterChainIT {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-02T10:15:30Z");
  private static final String SOME_IP = "198.51.100.42";
  private static final String SOME_EMAIL = "alice@example.com";
  private static final String SOME_PASSWORD = "wrong-password";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private AuthService authService;
  @MockitoBean private SigningKeyManager signingKeyManager;

  @Test
  void shouldAllowAnonymousAccessToLoginEndpoint() throws Exception {
    // given
    given(authService.login(SOME_EMAIL, SOME_PASSWORD, FIXED_NOW))
        .willReturn(new LoginOutcome.InvalidCredentials());
    var body = objectMapper.writeValueAsString(new LoginRequest(SOME_EMAIL, SOME_PASSWORD));

    // when/then — security must permit, controller returns 401 from credentials check
    mockMvc
        .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldAllowAnonymousAccessToJwksEndpoint() throws Exception {
    // given
    given(signingKeyManager.getJwkSet()).willReturn(Map.of("keys", List.of()));

    // when/then
    mockMvc
        .perform(get("/.well-known/jwks.json"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "max-age=3600, public"));
  }

  @Test
  void shouldDenyAnonymousAccessToProtectedRoutes() throws Exception {
    // when/then — any non-permitAll path is denied (Spring Security default
    // returns 403 since no AuthenticationEntryPoint sends a 401 challenge yet)
    mockMvc.perform(get("/api/v1/admin/anything")).andExpect(status().isForbidden());
  }

  @Test
  void shouldRateLimitLoginAfterFiveAttemptsFromSameIp() throws Exception {
    // given
    given(authService.login(SOME_EMAIL, SOME_PASSWORD, FIXED_NOW))
        .willReturn(new LoginOutcome.InvalidCredentials());
    var body = objectMapper.writeValueAsString(new LoginRequest(SOME_EMAIL, SOME_PASSWORD));

    // when — drain the bucket from a single IP
    for (var i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(remoteAddr(SOME_IP))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }

    // then — sixth call from same IP is rate-limited
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(remoteAddr(SOME_IP))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(
      String ip) {
    return req -> {
      req.setRemoteAddr(ip);
      return req;
    };
  }

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }
}
