package fasti.sh.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Security configuration including CORS and security headers.
 */
@Configuration
public class SecurityConfig {

    @Value("${cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * CORS configuration source.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Security headers filter for OWASP compliance.
     */
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                // Prevent clickjacking
                response.setHeader("X-Frame-Options", "DENY");

                // XSS protection
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-XSS-Protection", "1; mode=block");

                // Strict Transport Security (HSTS)
                response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");

                // Content Security Policy
                response.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "frame-ancestors 'none'");

                // Referrer Policy
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

                // Permissions Policy (formerly Feature-Policy)
                response.setHeader("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=(), payment=()");

                // Cache control for sensitive data
                if (request.getRequestURI().startsWith("/api/")) {
                    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                }

                filterChain.doFilter(request, response);
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                // Skip security headers for health checks and metrics
                return path.startsWith("/health") ||
                       path.startsWith("/ready") ||
                       path.startsWith("/live") ||
                       path.startsWith("/metrics") ||
                       path.startsWith("/prometheus");
            }
        };
    }
}
