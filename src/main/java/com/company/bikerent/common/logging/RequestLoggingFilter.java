package com.company.bikerent.common.logging;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Filter for request logging with correlation ID support. Adds a unique request ID to MDC for
 * tracing requests across logs.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String REQUEST_ID_MDC_KEY = "requestId";
  private static final String CLIENT_IP_MDC_KEY = "clientIp";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    String requestId = getOrGenerateRequestId(request);
    String clientIp = getClientIp(request);

    try {
      // Add context to MDC for logging
      MDC.put(REQUEST_ID_MDC_KEY, requestId);
      MDC.put(CLIENT_IP_MDC_KEY, maskIp(clientIp));

      // Add request ID to response header
      response.setHeader(REQUEST_ID_HEADER, requestId);

      // Log incoming request
      logRequest(request, requestId);

      // Continue filter chain
      filterChain.doFilter(request, response);

    } finally {
      // Log response
      logResponse(request, response, startTime, requestId);

      // Clean up MDC
      MDC.remove(REQUEST_ID_MDC_KEY);
      MDC.remove(CLIENT_IP_MDC_KEY);
    }
  }

  private String getOrGenerateRequestId(HttpServletRequest request) {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString().substring(0, 8);
    }
    return requestId;
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  private void logRequest(HttpServletRequest request, String requestId) {
    if (log.isDebugEnabled()) {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String queryString = request.getQueryString();

      String fullPath = uri + (queryString != null ? "?" + maskSensitiveParams(queryString) : "");

      log.debug("[{}] --> {} {}", requestId, method, fullPath);
    }
  }

  private void logResponse(
      HttpServletRequest request, HttpServletResponse response, long startTime, String requestId) {
    long duration = System.currentTimeMillis() - startTime;
    int status = response.getStatus();
    String method = request.getMethod();
    String uri = request.getRequestURI();

    if (status >= 500) {
      log.error("[{}] <-- {} {} {} ({}ms)", requestId, method, uri, status, duration);
    } else if (status >= 400) {
      log.warn("[{}] <-- {} {} {} ({}ms)", requestId, method, uri, status, duration);
    } else if (log.isDebugEnabled()) {
      log.debug("[{}] <-- {} {} {} ({}ms)", requestId, method, uri, status, duration);
    }
  }

  /** Mask sensitive parameters in query string. */
  private String maskSensitiveParams(String queryString) {
    if (queryString == null) {
      return null;
    }

    return queryString
        .replaceAll("(?i)(password|token|secret|key|authorization)=[^&]*", "$1=***")
        .replaceAll("(?i)(email)=[^&]*@[^&]*", "$1=***@***");
  }

  /** Mask IP address for privacy. */
  private String maskIp(String ip) {
    if (ip == null) {
      return "unknown";
    }
    int lastDot = ip.lastIndexOf('.');
    if (lastDot > 0) {
      return ip.substring(0, lastDot) + ".***";
    }
    return "***";
  }
}
