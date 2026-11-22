package com.niskora.common.web;

import com.niskora.common.ExpectedVersion;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  private static final String HEALTH_PATH = "/api/health";
  private static final String HEALTH_SCHEMA = "Health";

  /**
   * The fixed parts of the OpenAPI document (ENG-89). A fixed server keeps springdoc from writing
   * in the address the document happened to be generated on, so the committed file is the same on
   * every machine.
   */
  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info().title("niskora API").version("1"))
        .servers(List.of(new Server().url("/")))
        .paths(new Paths().addPathItem(HEALTH_PATH, health()))
        .components(new Components().addSchemas(HEALTH_SCHEMA, healthSchema()));
  }

  /**
   * Every field of every schema is required: a response always includes every field, and an empty
   * value is an explicit {@code null} (ENG-74), which springdoc marks from JSpecify's
   * {@code @Nullable}. The client types then hold no optional field that is never left out.
   */
  @Bean
  OpenApiCustomizer everyFieldIsRequired() {
    return openApi -> {
      Components components = openApi.getComponents();
      if (components == null || components.getSchemas() == null) {
        return;
      }
      for (Schema<?> schema : components.getSchemas().values()) {
        if (schema.getProperties() != null) {
          schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
      }
    };
  }

  /**
   * Every {@code POST} names the {@code Idempotency-Key} header it requires (ENG-85), so the client
   * types make the caller send one. {@link IdempotencyInterceptor} enforces it.
   */
  @Bean
  OperationCustomizer idempotencyKeyHeader() {
    return (operation, handler) -> {
      if (IdempotencyInterceptor.requiresKey(handler)) {
        operation.addParametersItem(
            new HeaderParameter()
                .name(IdempotencyInterceptor.HEADER)
                .description("Made once per user intent and sent again on a retry of it (ENG-85).")
                .required(true)
                .schema(new UUIDSchema()));
      }
      return operation;
    };
  }

  /**
   * Every change that takes an {@link ExpectedVersion} names the {@code If-Match} header it
   * requires (ENG-87), in place of the parameter itself, which springdoc would take for a query.
   * {@link ExpectedVersionResolver} enforces it.
   */
  @Bean
  OperationCustomizer ifMatchHeader() {
    return (operation, handler) -> {
      boolean takesVersion =
          Arrays.stream(handler.getMethodParameters())
              .anyMatch(parameter -> parameter.getParameterType() == ExpectedVersion.class);
      if (takesVersion) {
        operation.addParametersItem(
            new HeaderParameter()
                .name(ExpectedVersionResolver.HEADER)
                .description("The ETag of the version this change is based on (ENG-87).")
                .required(true)
                .schema(new StringSchema()));
      }
      return operation;
    };
  }

  static {
    SpringDocUtils.getConfig().addRequestWrapperToIgnore(ExpectedVersion.class);
  }

  /**
   * The one operational path (ENG-68). No controller serves it: the proxy forwards it to Actuator's
   * health endpoint on the management port, so springdoc cannot see it and it is written here by
   * hand, in the shape Actuator answers with (PLT-43).
   */
  private static PathItem health() {
    Content body =
        new Content()
            .addMediaType(
                "application/json",
                new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/" + HEALTH_SCHEMA)));
    return new PathItem()
        .get(
            new Operation()
                .operationId("health")
                .summary("Whether the backend is up")
                .responses(
                    new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Up").content(body))
                        .addApiResponse(
                            "503", new ApiResponse().description("Not up").content(body))));
  }

  /** Actuator's aggregate status, with no components or details. */
  private static Schema<?> healthSchema() {
    ObjectSchema schema = new ObjectSchema();
    List<String> codes =
        Stream.of(Status.UP, Status.DOWN, Status.OUT_OF_SERVICE, Status.UNKNOWN)
            .map(Status::getCode)
            .toList();
    schema.addProperty("status", new StringSchema()._enum(codes));
    schema.setRequired(List.of("status"));
    return schema;
  }
}
