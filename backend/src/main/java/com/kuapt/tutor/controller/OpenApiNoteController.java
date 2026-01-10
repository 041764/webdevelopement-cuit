package com.kuapt.tutor.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiNoteController {
  private final String serverPort;

  public OpenApiNoteController(@Value("${server.port}") String serverPort) {
    this.serverPort = serverPort;
  }

  @GetMapping("/openapi")
  public Map<String, String> openapi() {
    return Map.of(
        "swaggerUi", "http://localhost:" + serverPort + "/swagger-ui/index.html",
        "apiDocs", "http://localhost:" + serverPort + "/v3/api-docs");
  }
}

