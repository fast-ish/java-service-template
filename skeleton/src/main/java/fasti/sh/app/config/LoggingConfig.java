package fasti.sh.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Structured logging configuration with correlation IDs for distributed tracing.
 */
@Configuration
public class LoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String CLIENT_IP_KEY = "clientIp";
    public static final String USER_AGENT_KEY = "userAgent";
    public static final String METHOD_KEY = "httpMethod";
    public static final String PATH_KEY = "httpPath";

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Filter that extracts or generates correlation IDs and adds logging context.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                long startTime = System.currentTimeMillis();

                // Extract or generate correlation ID
                String correlationId = request.getHeader(CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = UUID.randomUUID().toString();
                }

                // Generate request-specific ID
                String requestId = UUID.randomUUID().toString().substring(0, 8);

                // Set MDC context for structured logging
                MDC.put(CORRELATION_ID_KEY, correlationId);
                MDC.put(REQUEST_ID_KEY, requestId);
                MDC.put(CLIENT_IP_KEY, getClientIp(request));
                MDC.put(USER_AGENT_KEY, sanitize(request.getHeader("User-Agent")));
                MDC.put(METHOD_KEY, request.getMethod());
                MDC.put(PATH_KEY, request.getRequestURI());
                MDC.put("service", applicationName);

                // Add correlation ID to response headers for tracing
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
                response.setHeader(REQUEST_ID_HEADER, requestId);

                try {
                    log.debug("Request started: {} {}",
                        request.getMethod(), request.getRequestURI());

                    filterChain.doFilter(request, response);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    MDC.put("durationMs", String.valueOf(duration));
                    MDC.put("httpStatus", String.valueOf(response.getStatus()));

                    log.info("Request completed: {} {} -> {} ({}ms)",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        duration);

                    MDC.clear();
                }
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                // Skip logging filter for health checks to reduce noise
                return path.equals("/health") ||
                       path.equals("/ready") ||
                       path.equals("/live") ||
                       path.startsWith("/actuator");
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        // Truncate and remove control characters for log safety
        return value.substring(0, Math.min(value.length(), 200))
                    .replaceAll("[\\x00-\\x1F]", "");
    }
}
