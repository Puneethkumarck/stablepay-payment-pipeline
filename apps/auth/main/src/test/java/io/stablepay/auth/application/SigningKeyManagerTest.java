package io.stablepay.auth.application;

import static io.stablepay.auth.domain.model.AuthDomainFixtures.SOME_INSTANT;
import static io.stablepay.auth.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.nimbusds.jose.JWSAlgorithm;
import io.stablepay.auth.domain.model.SigningKey;
import io.stablepay.auth.domain.port.SigningKeyRepository;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
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
    // given
    given(repository.findActive()).willReturn(Optional.empty());
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);
    var expected =
        SigningKey.builder()
            .kid("ignored")
            .privateKeyPem("ignored")
            .publicKeyPem("ignored")
            .algorithm("RS256")
            .createdAt(SOME_INSTANT)
            .isActive(true)
            .build();

    // when
    manager.initialize();

    // then
    var captor = ArgumentCaptor.forClass(SigningKey.class);
    then(repository).should().save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved)
        .usingRecursiveComparison()
        .ignoringFields("kid", "privateKeyPem", "publicKeyPem")
        .isEqualTo(expected);
    assertThat(saved.kid()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(saved.privateKeyPem()).startsWith("-----BEGIN PRIVATE KEY-----"); // gitleaks:allow
    assertThat(saved.publicKeyPem()).startsWith("-----BEGIN PUBLIC KEY-----");
  }

  @Test
  void doesNotGenerateWhenActiveKeyExists() {
    // given
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);

    // when
    manager.initialize();

    // then
    then(repository).should(never()).save(eqIgnoring(existing));
    assertThat(manager.getActiveKey()).isSameAs(existing);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getJwkSetReturnsRs256RsaKeyWithKid() {
    // given
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);
    manager.initialize();
    var publicKey = (RSAPublicKey) parsePublic(existing.publicKeyPem());
    var expectedJwk =
        new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey)
            .keyID(existing.kid())
            .algorithm(JWSAlgorithm.RS256)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .build()
            .toJSONObject();

    // when
    var jwks = manager.getJwkSet();

    // then
    var actualKeys = (List<Map<String, Object>>) jwks.get("keys");
    assertThat(actualKeys).hasSize(1);
    assertThat(actualKeys.get(0)).usingRecursiveComparison().isEqualTo(expectedJwk);
  }

  @Test
  void getActiveSignerSignsWithStoredPrivateKey() {
    // given
    var existing = generateActiveKey();
    given(repository.findActive()).willReturn(Optional.of(existing));
    var manager = new SigningKeyManager(repository, FIXED_CLOCK);
    manager.initialize();

    // when
    var signer = manager.getActiveSigner();

    // then
    assertThat(signer).isNotNull();
    assertThat(signer.supportedJWSAlgorithms()).contains(JWSAlgorithm.RS256);
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

  private static java.security.PublicKey parsePublic(String pem) {
    try {
      var stripped =
          pem.replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "")
              .replaceAll("\\s+", "");
      var bytes = Base64.getDecoder().decode(stripped);
      var spec = new java.security.spec.X509EncodedKeySpec(bytes);
      return java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
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
