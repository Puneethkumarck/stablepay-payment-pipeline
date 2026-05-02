package io.stablepay.auth.infrastructure.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.auth.application.SigningKeyManager;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

  @Mock private SigningKeyManager signingKeyManager;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new JwksController(signingKeyManager)).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldReturnJwkSetFromSigningKeyManager() throws Exception {
    // given
    var expected =
        Map.<String, Object>of(
            "keys",
            List.of(
                Map.of(
                    "kty", "RSA",
                    "e", "AQAB",
                    "n", "abcdef",
                    "kid", "kid-1",
                    "alg", "RS256",
                    "use", "sig")));
    given(signingKeyManager.getJwkSet()).willReturn(expected);

    // when
    var responseBody =
        mockMvc
            .perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "max-age=3600, public"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var actual = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

    // then
    Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
