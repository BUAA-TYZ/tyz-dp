package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("id.snowflake")
public class SnowflakeProperties {

    private Integer dataCenterId;

    private Integer workerId;
}
