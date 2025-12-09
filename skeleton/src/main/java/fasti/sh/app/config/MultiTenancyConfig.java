package fasti.sh.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Multi-tenancy configuration.
 *
 * Supports multiple tenant identification strategies:
 * - Header-based: X-Tenant-ID header
 * - Subdomain-based: tenant.example.com
 * - Path-based: /api/v1/tenants/{tenantId}/...
 * - JWT claim-based: Extract from authentication token
 */
@Configuration
public class MultiTenancyConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenancyConfig.class);

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Value("${multitenancy.enabled:false}")
    private boolean multiTenancyEnabled;

    @Value("${multitenancy.default-tenant:default}")
    private String defaultTenant;

    @Value("${multitenancy.require-tenant:false}")
    private boolean requireTenant;

    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/health", "/ready", "/live", "/metrics", "/prometheus",
        "/actuator", "/swagger-ui", "/api-docs"
    );

    @Bean
    @Order(1) // Run early, after correlation ID filter
    public OncePerRequestFilter tenantFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                if (!multiTenancyEnabled) {
                    filterChain.doFilter(request, response);
                    return;
                }

                try {
                    String tenantId = resolveTenantId(request);

                    if (tenantId == null && requireTenant) {
                        log.warn("Tenant ID required but not provided");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("{\"error\":\"Tenant ID is required\"}");
                        return;
                    }

                    if (tenantId == null) {
                        tenantId = defaultTenant;
                    }

                    TenantContext.setTenantId(tenantId);
                    log.debug("Set tenant context: {}", tenantId);

                    // Add tenant to response header for debugging
                    response.setHeader(TENANT_HEADER, tenantId);

                    filterChain.doFilter(request, response);
                } finally {
                    TenantContext.clear();
                }
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
            }
        };
    }

    private String resolveTenantId(HttpServletRequest request) {
        // Strategy 1: Header-based
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.trim();
        }

        // Strategy 2: Subdomain-based (tenant.example.com)
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!subdomain.equals("www") && !subdomain.equals("api")) {
                return subdomain;
            }
        }

        // Strategy 3: Path-based (/api/v1/tenants/{tenantId}/...)
        String path = request.getRequestURI();
        if (path.contains("/tenants/")) {
            String[] parts = path.split("/tenants/");
            if (parts.length > 1) {
                String remaining = parts[1];
                int nextSlash = remaining.indexOf('/');
                return nextSlash > 0 ? remaining.substring(0, nextSlash) : remaining;
            }
        }

        // Strategy 4: JWT claim (implement based on your auth setup)
        // Example: extract from SecurityContext or JWT token

        return null;
    }
}
