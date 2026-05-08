package io.stablepay.api.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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

  @Test
  void shouldAdd401And403ResponsesViaOperationCustomizer() {
    // given
    var customizer = config.globalAuthResponses();
    var operation = new Operation().responses(new ApiResponses());
    var expected =
        new ApiResponses()
            .addApiResponse("401", new ApiResponse().description("Missing or invalid JWT token"))
            .addApiResponse("403", new ApiResponse().description("Insufficient role permissions"));

    // when
    var result = customizer.customize(operation, null);

    // then
    assertThat(result.getResponses()).usingRecursiveComparison().isEqualTo(expected);
  }
}
