package com.example.botservice.client;

import static com.example.botservice.client.ClientResilienceSupport.rethrowBusinessOrRateLimit;
import static com.example.botservice.client.ClientResilienceSupport.unavailable;

import com.example.botservice.dto.OrderResponse;
import com.example.botservice.dto.PartnerRequest;
import com.example.botservice.dto.PartnerResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PartnerClient {

    private static final String CLIENT = "PartnerClient";

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
    @CircuitBreaker(name = "orderService", fallbackMethod = "getAllPartnersFallback")
    public List<PartnerResponse> getAllPartners() {
        return restClient.get().retrieve().body(new ParameterizedTypeReference<List<PartnerResponse>>() {});
    }

    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getPartnerByIdFallback")
    public PartnerResponse getPartnerById(UUID partnerId) {
        return restClient.get().uri("/{partnerId}", partnerId).retrieve().body(PartnerResponse.class);
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

    @SuppressWarnings("unused")
    private PartnerResponse createPartnerFallback(PartnerRequest request, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "createPartner(...)", ex);
    }

    @SuppressWarnings("unused")
    private List<PartnerResponse> getAllPartnersFallback(Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        log.warn("Circuit breaker fallback for getAllPartners(), empty list. Cause: {}", ex.toString());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private PartnerResponse getPartnerByIdFallback(UUID partnerId, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "getPartnerById(" + partnerId + ")", ex);
    }

    @SuppressWarnings("unused")
    private List<OrderResponse> getOrdersByPartnerIdFallback(UUID partnerId, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        log.warn(
                "Circuit breaker fallback for getOrdersByPartnerId({}), empty list. Cause: {}",
                partnerId,
                ex.toString());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private void deletePartnerFallback(UUID partnerId, Throwable ex) {
        rethrowBusinessOrRateLimit(ex);
        throw unavailable(CLIENT, "deletePartner(" + partnerId + ")", ex);
    }
}
