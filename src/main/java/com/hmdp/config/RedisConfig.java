package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/13 20:00
 */

@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 配置类
        Config config = new Config();
        // 配置Redis
        config.useSingleServer().setAddress("redis://192.168.134.128:6379").setPassword("123456");
        // 创建客户端
        return Redisson.create(config);
    }
}
