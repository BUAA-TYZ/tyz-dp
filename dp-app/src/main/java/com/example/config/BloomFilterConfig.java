package com.example.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    public static final String BF_SHOP = "bf:shop";

    @Bean
    public RBloomFilter<Long> shopBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BF_SHOP);
        // 预计插入量
        long expectedInsertions = 1000L;
        // 误判率
        double falsePositiveRate = 0.01D;
        // 初始化布隆过滤器，默认不覆盖已存在的布隆过滤器
        bloomFilter.tryInit(expectedInsertions, falsePositiveRate);
        return bloomFilter;
    }

}
