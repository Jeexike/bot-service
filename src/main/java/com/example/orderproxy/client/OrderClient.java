package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.exception.OrderServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClient {

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

    private OrderResponse getOrderFallback(UUID id, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("getOrder(" + id + ")", ex);
    }

    private List<OrderResponse> getOrdersFallback(Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        log.warn("Circuit breaker fallback for getOrders(), returning empty list stub. Cause: {}", ex.toString());
        return Collections.emptyList();
    }

    private OrderResponse createOrderFallback(OrderRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("createOrder(...)", ex);
    }

    private OrderResponse updateOrderFallback(UUID id, OrderRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("updateOrder(" + id + ")", ex);
    }

    private void deleteOrderFallback(UUID id, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("deleteOrder(" + id + ")", ex);
    }

    private static void rethrowBusinessOrRateLimit(Throwable ex) {
        if (ex instanceof RequestNotPermitted rnp) {
            throw rnp;
        }
        if (ex instanceof HttpClientErrorException hce) {
            throw hce;
        }
    }

    private OrderServiceUnavailableException unavailable(String operation, Throwable ex) {
        log.error("Resilience fallback triggered for OrderClient.{}: {}", operation, ex.toString());
        return new OrderServiceUnavailableException("Order service is currently unavailable", ex);
    }
}
