package com.company.bikerent.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  private static PostgreSQLContainer<?> postgres;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    String datasourceUrl = getDatasourceUrlOverride();
    String datasourceUsername = getDatasourceUsernameOverride();
    String datasourcePassword = getDatasourcePasswordOverride();

    if (datasourceUrl != null && !datasourceUrl.isBlank()) {
      registry.add("spring.datasource.url", () -> datasourceUrl);
      if (datasourceUsername != null && !datasourceUsername.isBlank()) {
        registry.add("spring.datasource.username", () -> datasourceUsername);
      }
      if (datasourcePassword != null && !datasourcePassword.isBlank()) {
        registry.add("spring.datasource.password", () -> datasourcePassword);
      }
    } else {
      postgres =
          new PostgreSQLContainer<>("postgres:15-alpine")
              .withDatabaseName("bikerent_test")
              .withUsername("test")
              .withPassword("test");
      postgres.start();

      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
    }

    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.flyway.enabled", () -> "false");
  }

  private static String getDatasourceUrlOverride() {
    String sysProp = System.getProperty("spring.datasource.url");
    if (sysProp != null && !sysProp.isBlank()) {
      return sysProp;
    }

    String env = System.getenv("SPRING_DATASOURCE_URL");
    if (env != null && !env.isBlank()) {
      return env;
    }

    Map<String, String> dotenv = readDotenv();
    String db = dotenv.getOrDefault("POSTGRES_DB", "bikerent");
    String port = dotenv.getOrDefault("POSTGRES_PORT", "5432");
    if (dotenv.containsKey("POSTGRES_PASSWORD") || dotenv.containsKey("POSTGRES_USER")) {
      return "jdbc:postgresql://localhost:" + port + "/" + db;
    }

    return null;
  }

  private static String getDatasourceUsernameOverride() {
    String sysProp = System.getProperty("spring.datasource.username");
    if (sysProp != null && !sysProp.isBlank()) {
      return sysProp;
    }

    String env = System.getenv("SPRING_DATASOURCE_USERNAME");
    if (env != null && !env.isBlank()) {
      return env;
    }

    Map<String, String> dotenv = readDotenv();
    if (dotenv.containsKey("POSTGRES_USER")) {
      return dotenv.get("POSTGRES_USER");
    }

    return null;
  }

  private static String getDatasourcePasswordOverride() {
    String sysProp = System.getProperty("spring.datasource.password");
    if (sysProp != null && !sysProp.isBlank()) {
      return sysProp;
    }

    String env = System.getenv("SPRING_DATASOURCE_PASSWORD");
    if (env != null && !env.isBlank()) {
      return env;
    }

    Map<String, String> dotenv = readDotenv();
    if (dotenv.containsKey("POSTGRES_PASSWORD")) {
      return dotenv.get("POSTGRES_PASSWORD");
    }

    return null;
  }

  private static Map<String, String> readDotenv() {
    Path path = Path.of(".env");
    if (!Files.exists(path)) {
      return Map.of();
    }

    try {
      String content = Files.readString(path, StandardCharsets.UTF_8);
      Map<String, String> values = new HashMap<>();
      for (String rawLine : content.split("\\R")) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        values.put(key, value);
      }
      return values;
    } catch (IOException e) {
      return Map.of();
    }
  }

  @BeforeEach
  void cleanDatabase() {
    jdbcTemplate.execute(
        "TRUNCATE TABLE admin_requests, refresh_token, repair, rental, payment, bicycle, station, technician, users "
            + "RESTART IDENTITY CASCADE");
  }
}
