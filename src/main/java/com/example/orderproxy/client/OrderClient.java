package com.example.orderproxy.client;

import static com.example.orderproxy.client.ClientResilienceSupport.rethrowBusinessOrRateLimit;
import static com.example.orderproxy.client.ClientResilienceSupport.unavailable;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClient {

    private static final String CLIENT = "OrderClient";

    private final RestClient restClient;

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderFallback")
    public OrderResponse getOrder(UUID id) {
        return restClient.get().uri("/{id}", id).retrieve().body(OrderResponse.class);
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersFallback")
    public List<OrderResponse> getOrders() {
        return restClient.get().uri("").retrieve().body(new ParameterizedTypeReference<List<OrderResponse>>() {});
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(OrderRequest request) {
        return restClient.post().body(request).retrieve().body(OrderResponse.class);
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderFallback")
    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        return restClient.put().uri("/{id}", id).body(request).retrieve().body(OrderResponse.class);
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "deleteOrderFallback")
    public void deleteOrder(UUID id) {
        restClient.delete().uri("/{id}", id).retrieve().toBodilessEntity();
    }

    // Resilience4j вызывает fallback по имени через reflection — сигнатура с Throwable обязательна
    @SuppressWarnings("unused")
    private OrderResponse getOrderFallback(UUID id, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "getOrder(" + id + ")", ex);
    }

    @SuppressWarnings("unused")
    private List<OrderResponse> getOrdersFallback(Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        log.warn("Circuit breaker fallback for getOrders(), returning empty list stub. Cause: {}", ex.toString());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private OrderResponse createOrderFallback(OrderRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "createOrder(...)", ex);
    }

    @SuppressWarnings("unused")
    private OrderResponse updateOrderFallback(UUID id, OrderRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "updateOrder(" + id + ")", ex);
    }

    @SuppressWarnings("unused")
    private void deleteOrderFallback(UUID id, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "deleteOrder(" + id + ")", ex);
    }
}