package com.example.config;

import com.example.impl.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties({SnowflakeProperties.class})
public class IdGeneratorAutoConfig {

    // 项目开始时间，减轻时间戳压力 (支持 69 年)
    // 2026-01-01 00:00:00
    private static final long DEFAULT_EPOCH_MILLIS = 1767196800L;

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(SnowflakeProperties snowflakeProperties) {
        return new SnowflakeIdGenerator(
                snowflakeProperties.getDataCenterId(), snowflakeProperties.getDataCenterId(), DEFAULT_EPOCH_MILLIS);
    }
}
