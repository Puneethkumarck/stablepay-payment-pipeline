package io.stablepay.api.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI stablepayOpenApi() {
    return new OpenAPI()
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
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  OperationCustomizer globalAuthResponses() {
    return (operation, handlerMethod) -> {
      operation
          .getResponses()
          .addApiResponse("401", new ApiResponse().description("Missing or invalid JWT token"))
          .addApiResponse("403", new ApiResponse().description("Insufficient role permissions"));
      return operation;
    };
  }
}
