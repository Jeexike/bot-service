package com.example.orderproxy.client;

import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PartnerClient {

    private final RestClient restClient;

    public PartnerClient(@Qualifier("partnerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public PartnerResponse createPartner(PartnerRequest request) {
        return restClient.post().body(request).retrieve().body(PartnerResponse.class);
    }

    public List<OrderResponse> getOrdersByPartnerId(UUID partnerId) {
        return restClient
                .get()
                .uri("/{partnerId}/orders", partnerId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderResponse>>() {});
    }

    public void deletePartner(UUID partnerId) {
        restClient.delete().uri("/{partnerId}", partnerId).retrieve().toBodilessEntity();
    }
}
