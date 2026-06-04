package com.example.orderproxy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rest-client")
public class RestClientProperties {
    private int connectTimeout;
    private int readTimeout;
    private int maxAttempts;
    private long backoffDelay;
}