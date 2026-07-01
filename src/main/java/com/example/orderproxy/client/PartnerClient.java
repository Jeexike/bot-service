package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
public class PartnerClient {

    private final WebClient webClient;

    public PartnerClient(@Qualifier("partnerWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public PartnerResponse createPartner(PartnerRequest request) {
        return webClient.post()
                .uri("")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PartnerResponse.class)
                .block();
    }

    public List<OrderResponse> getOrdersByPartnerId(UUID partnerId) {
        return webClient.get()
                .uri("/{partnerId}/orders", partnerId)
                .retrieve()
                .bodyToFlux(OrderResponse.class)
                .collectList()
                .block();
    }

    public void deletePartner(UUID partnerId) {
        webClient.delete()
                .uri("/{partnerId}", partnerId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}