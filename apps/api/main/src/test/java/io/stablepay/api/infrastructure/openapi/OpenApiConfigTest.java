package io.stablepay.api.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  private final OpenApiConfig config = new OpenApiConfig();

  @Test
  void shouldReturnOpenApiWithBearerAuthAndInfo() {
    // given
    var expected =
        new OpenAPI()
            .info(
                new Info()
                    .title("StablePay API")
                    .version("0.1.0-SNAPSHOT")
                    .description(
                        "Customer + admin + agent surfaces."
                            + " JWT bearer auth required for /api/v1/**."))
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .security(List.of(new SecurityRequirement().addList("bearerAuth")));

    // when
    var actual = config.stablepayOpenApi();

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
