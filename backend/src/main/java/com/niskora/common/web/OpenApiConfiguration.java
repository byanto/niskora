package com.niskora.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  /**
   * The fixed parts of the OpenAPI document (ENG-89). A fixed server keeps springdoc from writing
   * in the address the document happened to be generated on, so the committed file is the same on
   * every machine.
   */
  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info().title("niskora API").version("1"))
        .servers(List.of(new Server().url("/")));
  }
}
