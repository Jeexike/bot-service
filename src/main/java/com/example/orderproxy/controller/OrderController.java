package com.example.orderproxy.controller;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public OrderController(
            @Qualifier("restTemplate") RestTemplate restTemplate,
            @Value("${base.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/{id}")
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse getOrder(@PathVariable UUID id) {
        return restTemplate.getForObject(baseUrl + "/" + id, OrderResponse.class);
    }

    @GetMapping
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public List<OrderResponse> getOrders() {
        OrderResponse[] orders = restTemplate.getForObject(baseUrl, OrderResponse[].class);
        if (orders == null) {
            throw new IllegalStateException("Внешний сервис вернул пустое тело ответа");
        }
        return Arrays.asList(orders);
    }

    @PostMapping
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse createOrder(@RequestBody OrderRequest orderRequest) {
        return restTemplate.postForObject(baseUrl, orderRequest, OrderResponse.class);
    }

    @PutMapping("/{id}")
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse updateOrder(@PathVariable UUID id, @RequestBody OrderRequest orderRequest) {
        return restTemplate.exchange(baseUrl + "/" + id, HttpMethod.PUT, new HttpEntity<>(orderRequest), OrderResponse.class).getBody();
    }

    @DeleteMapping("/{id}")
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public void deleteOrder(@PathVariable UUID id) {
        restTemplate.delete(baseUrl + "/" + id);
    }
}
