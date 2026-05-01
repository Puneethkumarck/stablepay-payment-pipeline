package io.stablepay.auth.application;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.SigningKeyRepository;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SigningKeyManagerTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(SOME_INSTANT, ZoneOffset.UTC);

  @Mock private SigningKeyRepository repository;

  @Test
  void generatesAndPersistsKeypairWhenNoActiveKeyExists() {
    given(repository.findActive()).willReturn(Optional.empty());
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);

    manager.initialize();

    var captor = ArgumentCaptor.forClass(SigningKey.class);
    then(repository).should().save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved)
        .usingRecursiveComparison()
        .ignoringFields("kid", "privateKeyPem", "publicKeyPem")
        .isEqualTo(
            SigningKey.builder()
                .kid(saved.kid())
                .privateKeyPem(saved.privateKeyPem())
                .publicKeyPem(saved.publicKeyPem())
                .algorithm("RS256")
                .createdAt(SOME_INSTANT)
                .isActive(true)
                .build());
    assertThat(saved.kid()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(saved.privateKeyPem()).startsWith("-----BEGIN PRIVATE KEY-----"); // gitleaks:allow
    assertThat(saved.publicKeyPem()).startsWith("-----BEGIN PUBLIC KEY-----");
  }

  @Test
  void doesNotGenerateWhenActiveKeyExists() {
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);

    manager.initialize();

    then(repository).should(never()).save(any(SigningKey.class));
    assertThat(manager.getActiveKey()).isSameAs(existing);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getJwkSetReturnsRs256RsaKeyWithKid() {
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);
    manager.initialize();

    var jwks = manager.getJwkSet();

    var keys = (List<Map<String, Object>>) jwks.get("keys");
    assertThat(keys).hasSize(1);
    var jwk = keys.get(0);
    assertThat(jwk.get("kty")).isEqualTo("RSA");
    assertThat(jwk.get("kid")).isEqualTo(existing.kid());
    assertThat(jwk.get("alg")).isEqualTo("RS256");
    assertThat(jwk.get("n")).isInstanceOf(String.class);
    assertThat(jwk.get("e")).isInstanceOf(String.class);
  }

  @Test
  void getActiveSignerSignsWithStoredPrivateKey() throws Exception {
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);
    manager.initialize();

    var signer = manager.getActiveSigner();

    assertThat(signer).isNotNull();
    assertThat(signer.supportedJWSAlgorithms()).contains(com.nimbusds.jose.JWSAlgorithm.RS256);
  }

  private static SigningKey generateActiveKey() {
    try {
      var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      var pair = generator.generateKeyPair();
      return SigningKey.builder()
          .kid("test-kid-existing")
          .privateKeyPem(toPrivatePem(pair.getPrivate().getEncoded()))
          .publicKeyPem(toPublicPem(pair.getPublic().getEncoded()))
          .algorithm("RS256")
          .createdAt(SOME_INSTANT)
          .isActive(true)
          .build();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String toPrivatePem(byte[] pkcs8) {
    return "-----BEGIN PRIVATE KEY-----\n" // gitleaks:allow
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(pkcs8)
        + "\n-----END PRIVATE KEY-----\n";
  }

  private static String toPublicPem(byte[] x509) {
    return "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(x509)
        + "\n-----END PUBLIC KEY-----\n";
  }
}
