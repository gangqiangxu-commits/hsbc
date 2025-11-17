package com.hsbc.iwpb.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(@Value("${spring.redisson.address}") String redissonAddress) {
        Config config = new Config();
        config.useSingleServer().setAddress(redissonAddress);
        return Redisson.create(config);
    }
}
