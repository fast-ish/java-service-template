package fasti.sh.app.config;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature flags for the application.
 * Add new features here as needed.
 *
 * Usage:
 * <pre>
 * if (Features.NEW_FEATURE.isActive()) {
 *     // new feature logic
 * }
 * </pre>
 *
 * Or inject FeatureManager:
 * <pre>
 * @Autowired
 * private FeatureManager featureManager;
 *
 * if (featureManager.isActive(Features.NEW_FEATURE)) {
 *     // new feature logic
 * }
 * </pre>
 */
public enum Features implements Feature {

    @Label("Enable experimental API endpoints")
    EXPERIMENTAL_API,

    @EnabledByDefault
    @Label("Enable enhanced logging with request/response bodies")
    ENHANCED_LOGGING,

    @Label("Enable new search algorithm")
    NEW_SEARCH_ALGORITHM,

    @Label("Enable async processing for batch operations")
    ASYNC_BATCH_PROCESSING,

    @Label("Enable rate limiting")
    @EnabledByDefault
    RATE_LIMITING,

    @Label("Enable response caching")
    RESPONSE_CACHING;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
