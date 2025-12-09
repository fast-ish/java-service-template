package fasti.sh.app.locking;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed locking service.
 *
 * For production, replace the in-memory implementation with:
 * - Redis (Redisson, Lettuce)
 * - DynamoDB (optimistic locking)
 * - PostgreSQL (advisory locks)
 * - Consul/etcd
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);
    private final MeterRegistry meterRegistry;

    // In-memory lock store - replace with Redis/DB for production
    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    private final Counter lockAcquiredCounter;
    private final Counter lockFailedCounter;
    private final Counter lockReleasedCounter;

    public DistributedLockService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.lockAcquiredCounter = Counter.builder("distributed.lock.acquired")
            .description("Number of locks acquired")
            .register(meterRegistry);
        this.lockFailedCounter = Counter.builder("distributed.lock.failed")
            .description("Number of lock acquisition failures")
            .register(meterRegistry);
        this.lockReleasedCounter = Counter.builder("distributed.lock.released")
            .description("Number of locks released")
            .register(meterRegistry);
    }

    /**
     * Try to acquire a lock.
     *
     * @param lockName  Unique identifier for the lock
     * @param leaseTime How long to hold the lock
     * @return Optional containing lock token if acquired
     */
    public Optional<String> tryLock(String lockName, Duration leaseTime) {
        return tryLock(lockName, Duration.ZERO, leaseTime);
    }

    /**
     * Try to acquire a lock with waiting.
     *
     * @param lockName  Unique identifier for the lock
     * @param waitTime  How long to wait for the lock
     * @param leaseTime How long to hold the lock
     * @return Optional containing lock token if acquired
     */
    public Optional<String> tryLock(String lockName, Duration waitTime, Duration leaseTime) {
        Instant deadline = Instant.now().plus(waitTime);
        String lockToken = instanceId + "-" + UUID.randomUUID().toString().substring(0, 8);

        Timer.Sample sample = Timer.start(meterRegistry);

        do {
            // Clean up expired locks
            cleanExpiredLock(lockName);

            LockInfo existingLock = locks.get(lockName);
            if (existingLock == null) {
                LockInfo newLock = new LockInfo(lockToken, Instant.now().plus(leaseTime));
                LockInfo previous = locks.putIfAbsent(lockName, newLock);

                if (previous == null) {
                    lockAcquiredCounter.increment();
                    sample.stop(Timer.builder("distributed.lock.acquire.time")
                        .tag("success", "true")
                        .register(meterRegistry));
                    log.debug("Acquired lock: {} with token: {}", lockName, lockToken);
                    return Optional.of(lockToken);
                }
            }

            // Wait and retry if we have time
            if (Instant.now().isBefore(deadline)) {
                try {
                    Thread.sleep(50); // Small backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (Instant.now().isBefore(deadline));

        lockFailedCounter.increment();
        sample.stop(Timer.builder("distributed.lock.acquire.time")
            .tag("success", "false")
            .register(meterRegistry));
        log.debug("Failed to acquire lock: {}", lockName);
        return Optional.empty();
    }

    /**
     * Release a lock.
     *
     * @param lockName  The lock to release
     * @param lockToken The token received when acquiring the lock
     * @return true if the lock was released
     */
    public boolean unlock(String lockName, String lockToken) {
        LockInfo lock = locks.get(lockName);
        if (lock != null && lock.token().equals(lockToken)) {
            locks.remove(lockName, lock);
            lockReleasedCounter.increment();
            log.debug("Released lock: {} with token: {}", lockName, lockToken);
            return true;
        }
        log.warn("Failed to release lock: {} - token mismatch or lock expired", lockName);
        return false;
    }

    /**
     * Execute a task with a distributed lock.
     *
     * @param lockName  Lock identifier
     * @param leaseTime How long to hold the lock
     * @param task      The task to execute
     * @return Optional containing the result if lock was acquired
     */
    public <T> Optional<T> executeWithLock(String lockName, Duration leaseTime, Supplier<T> task) {
        return executeWithLock(lockName, Duration.ZERO, leaseTime, task);
    }

    /**
     * Execute a task with a distributed lock, waiting if necessary.
     */
    public <T> Optional<T> executeWithLock(String lockName, Duration waitTime,
                                            Duration leaseTime, Supplier<T> task) {
        Optional<String> lockToken = tryLock(lockName, waitTime, leaseTime);

        if (lockToken.isEmpty()) {
            return Optional.empty();
        }

        try {
            T result = task.get();
            return Optional.ofNullable(result);
        } finally {
            unlock(lockName, lockToken.get());
        }
    }

    /**
     * Execute a task with a distributed lock, throwing if lock cannot be acquired.
     */
    public <T> T executeWithLockOrThrow(String lockName, Duration leaseTime, Supplier<T> task) {
        return executeWithLock(lockName, leaseTime, task)
            .orElseThrow(() -> new LockAcquisitionException(
                "Failed to acquire lock: " + lockName));
    }

    private void cleanExpiredLock(String lockName) {
        locks.computeIfPresent(lockName, (key, lock) ->
            lock.expiresAt().isBefore(Instant.now()) ? null : lock);
    }

    /**
     * Extend the lease time of an existing lock.
     */
    public boolean extendLock(String lockName, String lockToken, Duration extension) {
        LockInfo lock = locks.get(lockName);
        if (lock != null && lock.token().equals(lockToken)) {
            LockInfo extended = new LockInfo(lockToken, Instant.now().plus(extension));
            return locks.replace(lockName, lock, extended);
        }
        return false;
    }

    record LockInfo(String token, Instant expiresAt) {}

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
