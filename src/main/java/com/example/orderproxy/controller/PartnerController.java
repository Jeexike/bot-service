package com.example.orderproxy.controller;

import com.example.orderproxy.client.PartnerClient;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners")
@RequiredArgsConstructor
@Tag(name = "Partners", description = "Операции с партнерами через proxy")
public class PartnerController implements PartnerApi {

    private final PartnerClient partnerClient;

    @Override
    public PartnerResponse createPartner(@Valid @RequestBody PartnerRequest request) {
        return partnerClient.createPartner(request);
    }

    @Override
    public List<PartnerResponse> getAllPartners() {
        return partnerClient.getAllPartners();
    }

    @Override
    public PartnerResponse getPartnerById(@PathVariable UUID partnerId) {
        return partnerClient.getPartnerById(partnerId);
    }

    @Override
    public List<OrderResponse> getOrdersByPartnerId(@PathVariable UUID partnerId) {
        return partnerClient.getOrdersByPartnerId(partnerId);
    }

    @Override
    public void deletePartner(@PathVariable UUID partnerId) {
        partnerClient.deletePartner(partnerId);
    }
}