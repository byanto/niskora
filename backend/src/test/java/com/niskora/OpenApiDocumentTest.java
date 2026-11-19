package com.niskora;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The backend is the source of truth for the API (ENG-89). This test writes the OpenAPI document
 * springdoc generates from the controllers into docs/api, and the frontend's types are generated
 * from that file. A change to it is committed; CI fails when the committed file differs.
 */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@DatabaseTest
// Every path but logging in needs a session, the document's included.
@WithMockUser
class OpenApiDocumentTest {

  /** The test runs in backend/, so the folder is one level up. */
  static final Path DOCUMENT = Path.of("../docs/api/openapi.yaml");

  @Autowired MockMvcTester mvc;

  @Test
  void writesTheOpenApiDocument() throws IOException {
    MvcTestResult result = mvc.get().uri("/v3/api-docs.yaml").exchange();
    assertThat(result).hasStatusOk();
    String document = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

    // Fixed values only: the document must come out the same on every machine and every run.
    assertThat(document)
        .startsWith("openapi: 3.1.")
        .contains("title: niskora API")
        .contains("- url: /")
        .doesNotContain("localhost")
        // The operational health path, which no controller serves (ENG-68).
        .contains("/api/health:")
        // A response always includes every field (ENG-74), so each one is required.
        .contains(
            """
                SessionUser:
                  type: object
                  properties:
                    fullName:
                      type: string
                    id:
                      type: string
                      format: uuid
                    loginName:
                      type: string
                  required:
                  - fullName
                  - id
                  - loginName
            """);

    Files.createDirectories(DOCUMENT.getParent());
    Files.writeString(DOCUMENT, document, StandardCharsets.UTF_8);
  }
}
