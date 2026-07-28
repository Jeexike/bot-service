package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OrderClient {

    private final RestClient restClient;

    public OrderResponse getOrder(UUID id) {
        return restClient.get().uri("/{id}", id).retrieve().body(OrderResponse.class);
    }

    public List<OrderResponse> getOrders() {
        return restClient.get().uri("").retrieve().body(new ParameterizedTypeReference<List<OrderResponse>>() {});
    }

    public OrderResponse createOrder(OrderRequest request) {
        return restClient.post().body(request).retrieve().body(OrderResponse.class);
    }

    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        return restClient.put().uri("/{id}", id).body(request).retrieve().body(OrderResponse.class);
    }

    public void deleteOrder(UUID id) {
        restClient.delete().uri("/{id}", id).retrieve().toBodilessEntity();
    }
}
