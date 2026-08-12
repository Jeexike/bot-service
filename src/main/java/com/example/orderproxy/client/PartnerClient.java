package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import com.example.orderproxy.exception.OrderServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PartnerClient {

    private final RestClient restClient;

    public PartnerClient(@Qualifier("partnerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "createPartnerFallback")
    public PartnerResponse createPartner(PartnerRequest request) {
        return restClient.post().body(request).retrieve().body(PartnerResponse.class);
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersByPartnerIdFallback")
    public List<OrderResponse> getOrdersByPartnerId(UUID partnerId) {
        return restClient
                .get()
                .uri("/{partnerId}/orders", partnerId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderResponse>>() {});
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "deletePartnerFallback")
    public void deletePartner(UUID partnerId) {
        restClient.delete().uri("/{partnerId}", partnerId).retrieve().toBodilessEntity();
    }

    private PartnerResponse createPartnerFallback(PartnerRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("createPartner(...)", ex);
    }

    private List<OrderResponse> getOrdersByPartnerIdFallback(UUID partnerId, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        log.warn(
                "Circuit breaker fallback for getOrdersByPartnerId({}), returning empty list stub. Cause: {}",
                partnerId,
                ex.toString());
        return Collections.emptyList();
    }

    private void deletePartnerFallback(UUID partnerId, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable("deletePartner(" + partnerId + ")", ex);
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
        log.error("Resilience fallback triggered for PartnerClient.{}: {}", operation, ex.toString());
        return new OrderServiceUnavailableException("Order service is currently unavailable", ex);
    }
}
