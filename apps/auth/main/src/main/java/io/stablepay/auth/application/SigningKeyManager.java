package io.stablepay.auth.application;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import io.stablepay.auth.domain.exception.SigningKeyGenerationException;
import io.stablepay.auth.domain.exception.SigningKeyParseException;
import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.SigningKeyRepository;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SigningKeyManager {

  private static final String ALGORITHM = "RS256";
  private static final int KEY_SIZE = 2048;
  private static final String PRIVATE_PEM_HEADER = "-----BEGIN PRIVATE KEY-----"; // gitleaks:allow
  private static final String PRIVATE_PEM_FOOTER = "-----END PRIVATE KEY-----";
  private static final String PUBLIC_PEM_HEADER = "-----BEGIN PUBLIC KEY-----";
  private static final String PUBLIC_PEM_FOOTER = "-----END PUBLIC KEY-----";

  private final SigningKeyRepository repository;
  private final Clock clock;

  private volatile SigningKey activeKey;
  private volatile RSAPrivateKey activePrivateKey;
  private volatile RSAPublicKey activePublicKey;
  private volatile RSASSASigner activeSigner;

  @PostConstruct
  public void initialize() {
    activeKey = repository.findActive().orElseGet(this::generateAndPersist);
    activePrivateKey = parsePrivatePem(activeKey.privateKeyPem());
    activePublicKey = parsePublicPem(activeKey.publicKeyPem());
    activeSigner = new RSASSASigner(activePrivateKey);
  }

  public SigningKey getActiveKey() {
    return activeKey;
  }

  public RSASSASigner getActiveSigner() {
    return activeSigner;
  }

  public Map<String, Object> getJwkSet() {
    var jwk =
        new RSAKey.Builder(activePublicKey)
            .keyID(activeKey.kid())
            .algorithm(JWSAlgorithm.RS256)
            .build()
            .toJSONObject();
    return Map.of("keys", List.of(jwk));
  }

  private SigningKey generateAndPersist() {
    try {
      var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(KEY_SIZE);
      var pair = generator.generateKeyPair();
      var kid = UUID.randomUUID().toString();
      var key =
          SigningKey.builder()
              .kid(kid)
              .privateKeyPem(toPrivatePem(pair.getPrivate().getEncoded()))
              .publicKeyPem(toPublicPem(pair.getPublic().getEncoded()))
              .algorithm(ALGORITHM)
              .createdAt(clock.instant())
              .isActive(true)
              .build();
      repository.save(key);
      log.info("Generated new RS256 signing key kid={}", kid);
      return key;
    } catch (NoSuchAlgorithmException e) {
      throw new SigningKeyGenerationException("Failed to generate RSA signing key", e);
    }
  }

  private static RSAPrivateKey parsePrivatePem(String pem) {
    try {
      var bytes = decodePem(pem, PRIVATE_PEM_HEADER, PRIVATE_PEM_FOOTER);
      var spec = new PKCS8EncodedKeySpec(bytes);
      return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new SigningKeyParseException("Failed to parse private key PEM", e);
    }
  }

  private static RSAPublicKey parsePublicPem(String pem) {
    try {
      var bytes = decodePem(pem, PUBLIC_PEM_HEADER, PUBLIC_PEM_FOOTER);
      var spec = new X509EncodedKeySpec(bytes);
      return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new SigningKeyParseException("Failed to parse public key PEM", e);
    }
  }

  private static String toPrivatePem(byte[] pkcs8) {
    return PRIVATE_PEM_HEADER
        + "\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(pkcs8)
        + "\n"
        + PRIVATE_PEM_FOOTER
        + "\n";
  }

  private static String toPublicPem(byte[] x509) {
    return PUBLIC_PEM_HEADER
        + "\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(x509)
        + "\n"
        + PUBLIC_PEM_FOOTER
        + "\n";
  }

  private static byte[] decodePem(String pem, String header, String footer) {
    var stripped = pem.replace(header, "").replace(footer, "").replaceAll("\\s+", "");
    return Base64.getDecoder().decode(stripped);
  }
}
