package com.niskora.common.web;

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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.stream.Stream;
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
