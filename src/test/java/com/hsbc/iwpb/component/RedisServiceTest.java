package com.hsbc.iwpb.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;

import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RedisServiceTest {
    private RedissonClient redissonClient;
    private Environment env;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        env = mock(Environment.class);
        when(env.getProperty(eq("spring.redis.host"), anyString())).thenReturn("localhost");
        when(env.getProperty(eq("spring.redis.port"), anyString())).thenReturn("6379");
        redisService = new RedisService(redissonClient, env);
    }

    @Test
    void testGetLock_success() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("account_lock_123")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        Lock result = redisService.getLock(123L);
        assertEquals(lock, result);
    }

    @Test
    void testGetLock_fail() {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("account_lock_123")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);
        Lock result = redisService.getLock(123L);
        assertNull(result);
    }

    @Test
    void testBuildLockKey() throws Exception {
        var method = RedisService.class.getDeclaredMethod("buildLockKey", long.class);
        method.setAccessible(true);
        String key = (String) method.invoke(null, 456L);
        assertEquals("account_lock_456", key);
    }

    @Test
    void testNextTransactionId() {
        var atomicLong = mock(org.redisson.api.RAtomicLong.class);
        when(redissonClient.getAtomicLong("transaction:seq")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(42L);
        long id = redisService.nextTransactionId();
        assertEquals(42L, id);
        verify(atomicLong).incrementAndGet();
    }
}