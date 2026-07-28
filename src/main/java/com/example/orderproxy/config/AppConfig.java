package com.example.orderproxy.config;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
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

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeout()));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
