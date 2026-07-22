package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class OrderClient {

    private final WebClient webClient;

    public OrderResponse getOrder(UUID id) {
        return webClient
                .get()
                .uri("/{id}", id)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .block();
    }

    public List<OrderResponse> getOrders() {
        return webClient
                .get()
                .uri("")
                .retrieve()
                .bodyToFlux(OrderResponse.class)
                .collectList()
                .block();
    }

    public OrderResponse createOrder(OrderRequest request) {
        return webClient
                .post()
                .uri("")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .block();
    }

    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        return webClient
                .put()
                .uri("/{id}", id)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .block();
    }

    public void deleteOrder(UUID id) {
        webClient.delete().uri("/{id}", id).retrieve().toBodilessEntity().block();
    }
}
