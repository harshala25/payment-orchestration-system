package com.yuno.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * API Key authentication filter.
 * 
 * Validates the X-API-Key header against the configured master key.
 * In production, this would validate against a database of merchant API keys.
 * 
 * Skips authentication for:
 * - Swagger UI and OpenAPI spec endpoints
 * - Actuator health endpoints
 * - OPTIONS preflight requests (CORS)
 */
@Component
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.security.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${app.security.master-api-key}")
    private String masterApiKey;

    private static final List<String> EXCLUDED_PATHS = List.of(
        "/swagger-ui", "/api-docs", "/v3/api-docs",
        "/actuator", "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip auth for excluded paths
        if (isExcluded(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(apiKeyHeader);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key for request: {} {}", request.getMethod(), path);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing API key. Provide X-API-Key header.");
            return;
        }

        if (!masterApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempt for: {} {}", request.getMethod(), path);
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid API key.");
            return;
        }

        // Set authentication context
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "merchant", null, List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"status\":%d,\"error\":\"AUTHENTICATION_ERROR\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            status, message, java.time.LocalDateTime.now()
        ));
    }
}
