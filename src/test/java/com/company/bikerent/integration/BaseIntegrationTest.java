package com.company.bikerent.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String datasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        String datasourceUsername = System.getenv("SPRING_DATASOURCE_USERNAME");
        String datasourcePassword = System.getenv("SPRING_DATASOURCE_PASSWORD");

        if (datasourceUrl != null && !datasourceUrl.isBlank()) {
            registry.add("spring.datasource.url", () -> datasourceUrl);
            if (datasourceUsername != null && !datasourceUsername.isBlank()) {
                registry.add("spring.datasource.username", () -> datasourceUsername);
            }
            if (datasourcePassword != null && !datasourcePassword.isBlank()) {
                registry.add("spring.datasource.password", () -> datasourcePassword);
            }
        } else {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
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

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE admin_requests, refresh_token, repair, rental, payment, bicycle, station, technician, users "
                        + "RESTART IDENTITY CASCADE");
    }
}
