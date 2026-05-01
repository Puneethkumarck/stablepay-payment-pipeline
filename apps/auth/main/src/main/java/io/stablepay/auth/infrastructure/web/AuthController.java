package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.application.AuthService;
import io.stablepay.auth.application.AuthService.LoginOutcome;
import io.stablepay.auth.client.ApiError;
import io.stablepay.auth.client.LoginRequest;
import io.stablepay.auth.client.RefreshRequest;
import io.stablepay.auth.client.TokenResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final String INVALID_LOGIN_CODE = "STBLPAY-1001";
  private static final String INVALID_REFRESH_CODE = "STBLPAY-1002";
  private static final String INVALID_CREDENTIALS_MSG = "Invalid credentials";

  private final AuthService authService;
  private final Clock clock;

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    var outcome = authService.login(request.email(), request.password(), clock.instant());
    return switch (outcome) {
      case LoginOutcome.Success success -> ResponseEntity.ok(toTokenResponse(success));
      case LoginOutcome.InvalidCredentials ignored ->
          ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(new ApiError(INVALID_LOGIN_CODE, INVALID_CREDENTIALS_MSG, clock.instant()));
    };
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
    var outcome = authService.refresh(request.refreshToken(), clock.instant());
    return switch (outcome) {
      case LoginOutcome.Success success -> ResponseEntity.ok(toTokenResponse(success));
      case LoginOutcome.InvalidCredentials ignored ->
          ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(new ApiError(INVALID_REFRESH_CODE, INVALID_CREDENTIALS_MSG, clock.instant()));
    };
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshRequest request) {
    authService.logout(request.refreshToken(), clock.instant());
  }

  private static TokenResponse toTokenResponse(LoginOutcome.Success success) {
    return new TokenResponse(
        success.accessToken(), success.refreshToken(), success.expiresIn().toSeconds());
  }
}
