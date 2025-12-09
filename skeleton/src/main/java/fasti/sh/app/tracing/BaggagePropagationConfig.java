package fasti.sh.app.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Configuration for OpenTelemetry baggage propagation.
 *
 * Baggage allows you to propagate custom context across service boundaries.
 * Unlike trace context (which is for tracing), baggage is for business context.
 *
 * Common use cases:
 * - User ID propagation
 * - Tenant ID propagation
 * - Feature flags
 * - Request origin tracking
 * - A/B test variant
 */
@Configuration
public class BaggagePropagationConfig {

    private static final Logger log = LoggerFactory.getLogger(BaggagePropagationConfig.class);

    // Standard baggage headers
    public static final String BAGGAGE_USER_ID = "baggage-user-id";
    public static final String BAGGAGE_TENANT_ID = "baggage-tenant-id";
    public static final String BAGGAGE_REQUEST_ORIGIN = "baggage-request-origin";
    public static final String BAGGAGE_FEATURE_FLAGS = "baggage-feature-flags";

    private static final Set<String> PROPAGATED_HEADERS = Set.of(
        BAGGAGE_USER_ID,
        BAGGAGE_TENANT_ID,
        BAGGAGE_REQUEST_ORIGIN,
        BAGGAGE_FEATURE_FLAGS,
        "X-B3-TraceId",
        "X-B3-SpanId",
        "X-B3-ParentSpanId",
        "X-B3-Sampled",
        "traceparent",
        "tracestate"
    );

    @Value("${tracing.baggage.enabled:true}")
    private boolean baggageEnabled;

    /**
     * Filter to extract and propagate baggage from incoming requests.
     */
    @Bean
    @Order(2) // After correlation ID and tenant filters
    public OncePerRequestFilter baggagePropagationFilter(Tracer tracer) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                if (!baggageEnabled) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract baggage from headers and add to current span/MDC
                extractBaggage(request, tracer);

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    // Clean up MDC
                    clearBaggageFromMdc();
                }
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                return path.startsWith("/health") ||
                       path.startsWith("/ready") ||
                       path.startsWith("/metrics");
            }
        };
    }

    private void extractBaggage(HttpServletRequest request, Tracer tracer) {
        Span currentSpan = tracer.currentSpan();

        // Extract user ID
        String userId = request.getHeader(BAGGAGE_USER_ID);
        if (userId != null) {
            MDC.put("userId", userId);
            if (currentSpan != null) {
                currentSpan.tag("user.id", userId);
            }
        }

        // Extract tenant ID
        String tenantId = request.getHeader(BAGGAGE_TENANT_ID);
        if (tenantId != null) {
            MDC.put("tenantId", tenantId);
            if (currentSpan != null) {
                currentSpan.tag("tenant.id", tenantId);
            }
        }

        // Extract request origin
        String origin = request.getHeader(BAGGAGE_REQUEST_ORIGIN);
        if (origin != null) {
            MDC.put("requestOrigin", origin);
            if (currentSpan != null) {
                currentSpan.tag("request.origin", origin);
            }
        }

        // Extract feature flags
        String featureFlags = request.getHeader(BAGGAGE_FEATURE_FLAGS);
        if (featureFlags != null) {
            MDC.put("featureFlags", featureFlags);
        }

        log.trace("Extracted baggage: userId={}, tenantId={}, origin={}",
            userId, tenantId, origin);
    }

    private void clearBaggageFromMdc() {
        MDC.remove("userId");
        MDC.remove("tenantId");
        MDC.remove("requestOrigin");
        MDC.remove("featureFlags");
    }

    /**
     * Utility to add baggage headers to outgoing requests.
     */
    public static class BaggageHeaders {

        private BaggageHeaders() {}

        /**
         * Get headers to propagate to downstream services.
         */
        public static java.util.Map<String, String> getHeadersToPropagate() {
            java.util.Map<String, String> headers = new java.util.HashMap<>();

            String userId = MDC.get("userId");
            if (userId != null) {
                headers.put(BAGGAGE_USER_ID, userId);
            }

            String tenantId = MDC.get("tenantId");
            if (tenantId != null) {
                headers.put(BAGGAGE_TENANT_ID, tenantId);
            }

            String origin = MDC.get("requestOrigin");
            if (origin != null) {
                headers.put(BAGGAGE_REQUEST_ORIGIN, origin);
            }

            String flags = MDC.get("featureFlags");
            if (flags != null) {
                headers.put(BAGGAGE_FEATURE_FLAGS, flags);
            }

            return headers;
        }
    }
}
