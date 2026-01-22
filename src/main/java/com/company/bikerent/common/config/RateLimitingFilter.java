package com.company.bikerent.common.config;

import com.company.bikerent.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j.
 * Limits requests per IP address for sensitive endpoints.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> paymentBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    private final int authRequestsPerMinute;
    private final int paymentRequestsPerMinute;
    private final int generalRequestsPerMinute;

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.auth.requests-per-minute:10}") int authRequestsPerMinute,
            @Value("${rate-limit.payment.requests-per-minute:30}") int paymentRequestsPerMinute,
            @Value("${rate-limit.general.requests-per-minute:100}") int generalRequestsPerMinute) {
        this.objectMapper = objectMapper;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.paymentRequestsPerMinute = paymentRequestsPerMinute;
        this.generalRequestsPerMinute = generalRequestsPerMinute;
        
        log.info("Rate limiting enabled: auth={}/min, payment={}/min, general={}/min",
                authRequestsPerMinute, paymentRequestsPerMinute, generalRequestsPerMinute);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        
        Bucket bucket = resolveBucket(clientIp, path);
        
        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", maskIp(clientIp), path);
            writeErrorResponse(request, response);
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Too many requests. Please try again later.")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private Bucket resolveBucket(String clientIp, String path) {
        if (path.startsWith("/api/v1/auth/login") || 
            path.startsWith("/api/v1/auth/register") ||
            path.startsWith("/api/v1/auth/refresh")) {
            return authBuckets.computeIfAbsent(clientIp, this::createAuthBucket);
        }
        
        if (path.startsWith("/api/v1/payments")) {
            return paymentBuckets.computeIfAbsent(clientIp, this::createPaymentBucket);
        }
        
        if (path.startsWith("/api/v1/")) {
            return generalBuckets.computeIfAbsent(clientIp, this::createGeneralBucket);
        }
        
        return null; // No rate limiting for non-API endpoints
    }

    private Bucket createAuthBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(authRequestsPerMinute, 
                        Refill.intervally(authRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createPaymentBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(paymentRequestsPerMinute, 
                        Refill.intervally(paymentRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createGeneralBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(generalRequestsPerMinute, 
                        Refill.intervally(generalRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Get client IP address, considering proxy headers.
     */
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

    /**
     * Mask IP for logging (privacy).
     */
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
