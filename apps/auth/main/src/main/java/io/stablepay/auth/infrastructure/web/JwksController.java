package io.stablepay.auth.infrastructure.web;

import io.stablepay.auth.application.SigningKeyManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

  private final SigningKeyManager signingKeyManager;

  @GetMapping(path = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> jwks() {
    return signingKeyManager.getJwkSet();
  }
}
