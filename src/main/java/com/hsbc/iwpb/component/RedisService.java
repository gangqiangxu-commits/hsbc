package com.hsbc.iwpb.component;

import java.util.concurrent.locks.Lock;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    private final RedissonClient redissonClient;

    @Autowired
    public RedisService(RedissonClient redissonClient, Environment env) {
        // Log the redis host and port property at bean creation time
        String redisHost = env.getProperty("spring.redis.host", "NOT_SET");
        String redisPort = env.getProperty("spring.redis.port", "NOT_SET");
        log.info("[DIAG] spring.redis.host property in RedisService: {}", redisHost);
        log.info("[DIAG] spring.redis.port property in RedisService: {}", redisPort);
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
        RAtomicLong seq = redissonClient.getAtomicLong("transaction:seq");
        long id = seq.incrementAndGet();
        log.info("Generated transaction id: {}", id);
        return id;
    }
}