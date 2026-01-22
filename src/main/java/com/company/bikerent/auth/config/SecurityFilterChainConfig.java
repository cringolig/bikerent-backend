package com.company.bikerent.auth.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.company.bikerent.common.security.RestAccessDeniedHandler;
import com.company.bikerent.common.security.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

/**
 * Security filter chain configuration. Configures CORS, CSRF, security headers, and authorization
 * rules.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityFilterChainConfig {

  /** Public endpoints that don't require authentication. */
  private static final String[] PUBLIC_URLS = {
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout",
    "/api/v1/api-docs/**",
    "/api/v1/api-docs",
    "/v3/api-docs/**",
    "/v3/api-docs",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/swagger-resources/**",
    "/webjars/**",
    "/actuator/health",
    "/actuator/info"
  };

  /** Admin-only endpoints. */
  private static final String[] ADMIN_URLS = {"/api/v1/admin-requests/**", "/api/v1/users/**"};

  /** Technician and Admin endpoints (repair management). */
  private static final String[] TECH_ADMIN_URLS = {"/api/v1/repairs/**", "/api/v1/technicians/**"};

  /** WebSocket endpoints. */
  private static final String[] WEBSOCKET_URLS = {"/ws/**"};

  private final AuthenticationProvider authenticationProvider;
  private final JwtFilter jwtFilter;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  private final RestAccessDeniedHandler restAccessDeniedHandler;

  @Value("${cors.allowed-origins:http://localhost:3000}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Disable CSRF for stateless API
        .csrf(AbstractHttpConfigurer::disable)

        // Configure CORS with whitelist
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // Configure security headers
        .headers(
            headers ->
                headers
                    // HSTS - enforce HTTPS
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    // Prevent MIME type sniffing
                    .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::and)
                    // XSS protection
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                org.springframework.security.web.header.writers
                                    .XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    // Prevent clickjacking
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    // Referrer policy
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    // Content Security Policy
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'"))
                    // Permissions policy
                    .permissionsPolicy(
                        permissions ->
                            permissions.policy("geolocation=(), microphone=(), camera=()")))

        // Ensure consistent JSON errors for 401/403
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))

        // Configure authorization rules
        .authorizeHttpRequests(
            request ->
                request
                    // Public endpoints
                    .requestMatchers(PUBLIC_URLS)
                    .permitAll()

                    // WebSocket endpoints
                    .requestMatchers(WEBSOCKET_URLS)
                    .permitAll()

                    // OPTIONS requests for CORS preflight
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()

                    // Admin-only endpoints
                    .requestMatchers(ADMIN_URLS)
                    .hasRole("ADMIN")

                    // Tech and Admin endpoints
                    .requestMatchers(TECH_ADMIN_URLS)
                    .hasAnyRole("TECH", "ADMIN")

                    // Payment endpoints - authenticated users
                    .requestMatchers("/api/v1/payments/**")
                    .authenticated()

                    // Bicycle management - Admin only for write operations
                    .requestMatchers(HttpMethod.POST, "/api/v1/bicycles/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/bicycles/**")
                    .hasAnyRole("TECH", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/bicycles/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/bicycles/**")
                    .authenticated()

                    // Station management - Admin only for write operations
                    .requestMatchers(HttpMethod.POST, "/api/v1/stations/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/stations/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/stations/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/stations/**")
                    .authenticated()

                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())

        // Stateless session management
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Add authentication provider and JWT filter
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Parse allowed origins from configuration
    List<String> origins = Arrays.asList(allowedOrigins.split(","));
    configuration.setAllowedOrigins(origins);

    // Allowed HTTP methods
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // Allowed headers
    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));

    // Exposed headers (client can access these)
    configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // Cache preflight response for 1 hour
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
