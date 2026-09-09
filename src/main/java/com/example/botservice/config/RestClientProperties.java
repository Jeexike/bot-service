package com.example.botservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rest-client")
public class RestClientProperties {

    private String baseUrl;
    private String partnersBaseUrl;
    private int connectTimeout;
    private int readTimeout;
}
