package fasti.sh.app.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async processing configuration with MDC context propagation.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final MeterRegistry meterRegistry;

    public AsyncConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-exec-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        // Register metrics
        meterRegistry.gauge("async.executor.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize);
        meterRegistry.gauge("async.executor.active.count", executor, ThreadPoolTaskExecutor::getActiveCount);
        meterRegistry.gauge("async.executor.queue.size", executor,
            e -> e.getThreadPoolExecutor().getQueue().size());

        return executor;
    }

    /**
     * Task decorator that propagates MDC context to async threads.
     * Essential for maintaining trace IDs across async boundaries.
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught exception in async method: {} with params: {}",
                method.getName(), params, throwable);
            meterRegistry.counter("async.uncaught.exceptions",
                "method", method.getName(),
                "exception", throwable.getClass().getSimpleName()).increment();
        };
    }

    private RejectedExecutionHandler rejectedExecutionHandler() {
        return (runnable, executor) -> {
            log.warn("Task rejected from async executor. Queue full. " +
                "Pool size: {}, Active: {}, Queue size: {}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size());
            meterRegistry.counter("async.rejected.tasks").increment();

            // Caller-runs policy: execute in calling thread as fallback
            if (!executor.isShutdown()) {
                runnable.run();
            }
        };
    }

    /**
     * Dedicated executor for background jobs (scheduled tasks, cleanup, etc.).
     */
    @Bean(name = "backgroundExecutor")
    public Executor backgroundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("background-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
