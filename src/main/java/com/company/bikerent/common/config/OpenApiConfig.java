package com.company.bikerent.common.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.company.bikerent.common.dto.ErrorResponse;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/** OpenAPI/Swagger configuration with security schemes and example responses. */
@Configuration
public class OpenApiConfig {

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  @Bean
  public OpenAPI customOpenAPI() {
    final String bearerAuthScheme = "bearerAuth";

    Map<String, Object> validationFieldErrorExample = new LinkedHashMap<>();
    validationFieldErrorExample.put("field", "username");
    validationFieldErrorExample.put("message", "Username is required");
    validationFieldErrorExample.put("rejectedValue", null);

    return new OpenAPI()
        // API Information
        .info(
            new Info()
                .title("Bike Rental API")
                .version("1.0.0")
                .description(
                    """
                                REST API for Bike Rental Service.

                                ## Authentication

                                This API uses JWT Bearer tokens for authentication. To authenticate:

                                1. Register a new account using `/api/v1/auth/register` or login using `/api/v1/auth/login`
                                2. Use the returned `access_token` in the `Authorization` header as `Bearer <token>`
                                3. When the access token expires, use the `refresh_token` with `/api/v1/auth/refresh` to get new tokens

                                ## Rate Limiting

                                - Authentication endpoints: 10 requests/minute
                                - Payment endpoints: 30 requests/minute
                                - General API endpoints: 100 requests/minute

                                ## Error Handling

                                All errors follow a consistent format with `timestamp`, `status`, `error`, `message`, and `path` fields.
                                """)
                .contact(
                    new Contact()
                        .name("Bike Rental Team")
                        .email("support@bikerent.com")
                        .url("https://github.com/company/bikerent"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/licenses/MIT")))

        // Servers
        .servers(
            List.of(
                new Server()
                    .url("http://localhost:8080" + contextPath)
                    .description("Local Development"),
                new Server()
                    .url("https://api.bikerent.com" + contextPath)
                    .description("Production")))

        // Tags for grouping endpoints
        .tags(
            List.of(
                new Tag()
                    .name("Authentication")
                    .description("User authentication and token management"),
                new Tag().name("Rentals").description("Bicycle rental operations"),
                new Tag().name("Bicycles").description("Bicycle management"),
                new Tag().name("Stations").description("Station management"),
                new Tag().name("Payments").description("Payment and balance operations"),
                new Tag()
                    .name("Repairs")
                    .description("Bicycle repair management (Tech/Admin only)"),
                new Tag().name("Technicians").description("Technician management (Admin only)")))

        // Security Requirement (global)
        .addSecurityItem(new SecurityRequirement().addList(bearerAuthScheme))

        // Components (security schemes, schemas, examples)
        .components(
            new Components()
                // Security Schemes
                .addSecuritySchemes(
                    bearerAuthScheme,
                    new SecurityScheme()
                        .name(bearerAuthScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT access token. Obtain via `/api/v1/auth/login` or `/api/v1/auth/register`"))

                // Common Response Schemas
                .addSchemas(
                    "ErrorResponse",
                    new Schema<ErrorResponse>()
                        .type("object")
                        .description("Standard error response format")
                        .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                        .addProperty("status", new Schema<>().type("integer"))
                        .addProperty("error", new Schema<>().type("string"))
                        .addProperty("message", new Schema<>().type("string"))
                        .addProperty("path", new Schema<>().type("string"))
                        .addProperty("fieldErrors", new Schema<>().type("array")))

                // Common Response Examples
                .addResponses(
                    "UnauthorizedError",
                    new ApiResponse()
                        .description("Authentication required")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref("#/components/schemas/ErrorResponse"))
                                        .addExamples(
                                            "default",
                                            new Example()
                                                .value(
                                                    Map.of(
                                                        "timestamp", "2024-01-15T10:30:00",
                                                        "status", 401,
                                                        "error", "Unauthorized",
                                                        "message",
                                                            "Authentication failed: Invalid token",
                                                        "path", "/api/v1/rentals"))))))
                .addResponses(
                    "ForbiddenError",
                    new ApiResponse()
                        .description("Insufficient permissions")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref("#/components/schemas/ErrorResponse"))
                                        .addExamples(
                                            "default",
                                            new Example()
                                                .value(
                                                    Map.of(
                                                        "timestamp", "2024-01-15T10:30:00",
                                                        "status", 403,
                                                        "error", "Forbidden",
                                                        "message", "Access denied",
                                                        "path", "/api/v1/repairs"))))))
                .addResponses(
                    "NotFoundError",
                    new ApiResponse()
                        .description("Resource not found")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref("#/components/schemas/ErrorResponse"))
                                        .addExamples(
                                            "default",
                                            new Example()
                                                .value(
                                                    Map.of(
                                                        "timestamp", "2024-01-15T10:30:00",
                                                        "status", 404,
                                                        "error", "Not Found",
                                                        "message", "Bicycle not found with id: 123",
                                                        "path", "/api/v1/bicycles/123"))))))
                .addResponses(
                    "ValidationError",
                    new ApiResponse()
                        .description("Validation failed")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref("#/components/schemas/ErrorResponse"))
                                        .addExamples(
                                            "default",
                                            new Example()
                                                .value(
                                                    Map.of(
                                                        "timestamp", "2024-01-15T10:30:00",
                                                        "status", 400,
                                                        "error", "Bad Request",
                                                        "message", "Validation failed",
                                                        "path", "/api/v1/auth/register",
                                                        "fieldErrors",
                                                            List.of(
                                                                validationFieldErrorExample)))))))
                .addResponses(
                    "TooManyRequests",
                    new ApiResponse()
                        .description("Rate limit exceeded")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .addExamples(
                                            "default",
                                            new Example()
                                                .value(
                                                    Map.of(
                                                        "error",
                                                        "Too many requests. Please try again later.")))))));
  }
}
