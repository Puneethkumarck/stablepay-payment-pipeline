package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.application.SigningKeyManager;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

  private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);

  private final SigningKeyManager signingKeyManager;

  @GetMapping(path = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> jwks() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(JWKS_CACHE_TTL).cachePublic())
        .body(signingKeyManager.getJwkSet());
  }
}
