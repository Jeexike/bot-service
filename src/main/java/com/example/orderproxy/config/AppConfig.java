package com.example.orderproxy.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
@RequiredArgsConstructor
public class AppConfig {

    private final RestClientProperties properties;

    @Bean
    public RestClient restClient() {
        return buildRestClient(properties.getBaseUrl());
    }

    @Bean
    @Qualifier("partnerRestClient")
    public RestClient partnerRestClient() {
        return buildRestClient(properties.getPartnersBaseUrl());
    }

    private RestClient buildRestClient(String baseUrl) {

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .withReadTimeout(Duration.ofMillis(properties.getReadTimeout()));

        ClientHttpRequestFactory requestFactory =
                ClientHttpRequestFactoryBuilder.jdk().build(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
