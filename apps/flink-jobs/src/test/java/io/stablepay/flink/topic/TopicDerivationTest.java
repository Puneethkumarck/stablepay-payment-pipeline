package io.stablepay.flink.topic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicDerivationTest {

  @Test
  void shouldDeriveCryptoFlowTypeForChainTopics() {
    assertThat(TopicDerivation.deriveFlowType("payment.payout.crypto.v1")).isEqualTo("CRYPTO");
    assertThat(TopicDerivation.deriveFlowType("chain.tx.confirmed.v1")).isEqualTo("CRYPTO");
  }

  @Test
  void shouldDeriveFiatFlowType() {
    assertThat(TopicDerivation.deriveFlowType("payment.payin.fiat.v1")).isEqualTo("FIAT");
  }

  @Test
  void shouldDefaultToMixedForUnclassifiedTopics() {
    assertThat(TopicDerivation.deriveFlowType("internal.transfer.v1")).isEqualTo("MIXED");
  }

  @Test
  void shouldDeriveDirectionFromPayinAndPayout() {
    assertThat(TopicDerivation.deriveDirection("payment.payin.fiat.v1")).isEqualTo("INBOUND");
    assertThat(TopicDerivation.deriveDirection("payment.payout.crypto.v1")).isEqualTo("OUTBOUND");
  }

  @Test
  void shouldDefaultToInternalForUnclassifiedDirection() {
    assertThat(TopicDerivation.deriveDirection("screening.result.v1")).isEqualTo("INTERNAL");
  }
}
