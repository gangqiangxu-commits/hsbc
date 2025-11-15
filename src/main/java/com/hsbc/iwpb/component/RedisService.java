package com.hsbc.iwpb.component;

import java.util.concurrent.locks.Lock;
import java.time.Duration;
import java.util.function.Supplier;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@Component
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    private final RedissonClient redissonClient;

    @Autowired
    public RedisService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public Lock getLock(long accountNumber) {
		var lock = redissonClient.getLock(buildLockKey(accountNumber));
		if (lock.tryLock()) {
			return lock;
		}
		return null;
	}
    
    private static String buildLockKey(long accountNumber) {
		return "account_lock_" + accountNumber;
	}
    
    /**
     * Generate the next global transaction id using Redis-backed atomic counter.
     * Uses key: "transaction:seq".
     *
     * @return next transaction id (monotonic increasing long)
     */
    public long nextTransactionId() {
    	RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
    	final Retry retry = Retry.of("transactionServiceRetry", retryConfig);
    	
        Supplier<Long> supplier = Retry.decorateSupplier(retry, () -> {
            RAtomicLong seq = redissonClient.getAtomicLong("transaction:seq");
            long id = seq.incrementAndGet();
            log.info("Generated transaction id: {}", id);
            return id;
        });

        // Execute supplier — Retry will retry on RuntimeExceptions
        return supplier.get();
    }
    
}