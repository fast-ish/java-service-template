package fasti.sh.app.versioning;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Interceptor that adds deprecation headers to responses.
 *
 * Implements RFC 8594 (Sunset Header) and draft-deprecation-header.
 */
@Component
public class ApiDeprecationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiDeprecationInterceptor.class);
    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;

    private final MeterRegistry meterRegistry;

    public ApiDeprecationInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check method-level annotation first, then class-level
        ApiDeprecation deprecation = handlerMethod.getMethodAnnotation(ApiDeprecation.class);
        if (deprecation == null) {
            deprecation = handlerMethod.getBeanType().getAnnotation(ApiDeprecation.class);
        }

        if (deprecation == null) {
            return true;
        }

        addDeprecationHeaders(response, deprecation, request);

        if (deprecation.logUsage()) {
            logDeprecatedUsage(request, deprecation);
        }

        // Check if past sunset date
        LocalDate sunsetDate = LocalDate.parse(deprecation.sunset());
        if (LocalDate.now().isAfter(sunsetDate)) {
            log.warn("Request to sunset API: {} {} (sunset: {})",
                request.getMethod(), request.getRequestURI(), deprecation.sunset());
            // Optionally return 410 Gone after sunset
            // response.setStatus(HttpServletResponse.SC_GONE);
            // return false;
        }

        return true;
    }

    private void addDeprecationHeaders(HttpServletResponse response,
                                       ApiDeprecation deprecation,
                                       HttpServletRequest request) {

        // Deprecation header (Unix timestamp with @ prefix)
        LocalDate sinceDate = LocalDate.parse(deprecation.since());
        long sinceEpoch = sinceDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        response.setHeader("Deprecation", "@" + sinceEpoch);

        // Sunset header (RFC 1123 date)
        LocalDate sunsetDate = LocalDate.parse(deprecation.sunset());
        String sunsetFormatted = sunsetDate.atStartOfDay()
            .atOffset(ZoneOffset.UTC)
            .format(HTTP_DATE);
        response.setHeader("Sunset", sunsetFormatted);

        // Link header for successor
        if (!deprecation.successor().isEmpty()) {
            response.addHeader("Link",
                String.format("<%s>; rel=\"successor-version\"", deprecation.successor()));
        }

        // Custom header with deprecation message
        if (!deprecation.message().isEmpty()) {
            response.setHeader("X-Deprecation-Notice", deprecation.message());
        }

        // Warning header (RFC 7234)
        response.setHeader("Warning",
            String.format("299 - \"Deprecated API: sunset on %s\"", deprecation.sunset()));
    }

    private void logDeprecatedUsage(HttpServletRequest request, ApiDeprecation deprecation) {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String clientId = request.getHeader("X-Client-ID");

        log.info("Deprecated API usage: endpoint={}, sunset={}, client={}",
            endpoint, deprecation.sunset(), clientId);

        Counter.builder("api.deprecated.usage")
            .tag("endpoint", endpoint)
            .tag("sunset", deprecation.sunset())
            .tag("client", clientId != null ? clientId : "unknown")
            .register(meterRegistry)
            .increment();
    }
}
