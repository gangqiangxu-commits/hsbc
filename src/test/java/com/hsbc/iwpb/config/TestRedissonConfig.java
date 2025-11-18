package com.hsbc.iwpb.config;

import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestRedissonConfig {
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = Mockito.mock(RedissonClient.class);
        RLock mockLock = Mockito.mock(RLock.class);
        when(mockClient.getLock(anyString())).thenReturn(mockLock);
        when(mockLock.tryLock()).thenReturn(true);
        Mockito.doNothing().when(mockLock).unlock();

        RAtomicLong mockAtomicLong = Mockito.mock(RAtomicLong.class);
        AtomicLong counter = new AtomicLong(0);
        when(mockClient.getAtomicLong(anyString())).thenReturn(mockAtomicLong);
        when(mockAtomicLong.incrementAndGet()).thenAnswer(invocation -> counter.incrementAndGet());

        return mockClient;
    }
}