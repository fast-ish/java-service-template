package fasti.sh.app.config;

import org.slf4j.MDC;

/**
 * Thread-local holder for multi-tenancy context.
 *
 * Usage:
 * <pre>
 * // Set tenant at request entry (via filter/interceptor)
 * TenantContext.setTenantId("tenant-123");
 *
 * // Access tenant anywhere in the request
 * String tenantId = TenantContext.getTenantId();
 *
 * // Clear at request end
 * TenantContext.clear();
 * </pre>
 */
public final class TenantContext {

    private static final String TENANT_ID_KEY = "tenantId";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Set the current tenant ID.
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
        MDC.put(TENANT_ID_KEY, tenantId);
    }

    /**
     * Get the current tenant ID.
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Get the current tenant ID or a default value.
     */
    public static String getTenantIdOrDefault(String defaultValue) {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : defaultValue;
    }

    /**
     * Check if a tenant is set.
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clear the tenant context.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
        MDC.remove(TENANT_ID_KEY);
    }

    /**
     * Execute a runnable with a specific tenant context.
     */
    public static void runWithTenant(String tenantId, Runnable runnable) {
        String previousTenant = getTenantId();
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previousTenant != null) {
                setTenantId(previousTenant);
            } else {
                clear();
            }
        }
    }
}
