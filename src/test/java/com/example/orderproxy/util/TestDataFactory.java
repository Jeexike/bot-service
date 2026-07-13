package com.example.orderproxy.util;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;

import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static OrderRequest orderRequest() {
        return orderRequest("Order", "Moscow", "SPB", null);
    }

    public static OrderRequest orderRequest(UUID partnerId) {
        return orderRequest("Order", "Moscow", "SPB", partnerId);
    }

    public static OrderRequest orderRequest(String name, String source, String destination, UUID partnerId) {
        OrderRequest request = new OrderRequest();
        request.setName(name);
        request.setSource(source);
        request.setDestination(destination);
        request.setPartnerId(partnerId);
        return request;
    }

    public static OrderRequest invalidOrderRequest() {
        return orderRequest("", "A", "B", null);
    }

    public static OrderResponse orderResponse() {
        return orderResponse(UUID.randomUUID(), "Order", "Moscow", "SPB", null);
    }

    public static OrderResponse orderResponse(UUID id) {
        return orderResponse(id, "Order", "Moscow", "SPB", null);
    }

    public static OrderResponse orderResponse(UUID id, UUID partnerId) {
        return orderResponse(id, "Order", "Moscow", "SPB", partnerId);
    }

    public static OrderResponse orderResponse(UUID id, String name, String source, String destination, UUID partnerId) {
        OrderResponse response = new OrderResponse();
        response.setId(id);
        response.setName(name);
        response.setSource(source);
        response.setDestination(destination);
        response.setPartnerId(partnerId);
        return response;
    }

    public static PartnerRequest partnerRequest() {
        return partnerRequest("Partner", "partner@test.com");
    }

    public static PartnerRequest partnerRequest(String name, String email) {
        PartnerRequest request = new PartnerRequest();
        request.setName(name);
        request.setEmail(email);
        return request;
    }

    public static PartnerRequest invalidPartnerRequest() {
        return partnerRequest("", "not-email");
    }

    public static PartnerResponse partnerResponse() {
        return partnerResponse(UUID.randomUUID(), "Partner", "partner@test.com");
    }

    public static PartnerResponse partnerResponse(UUID id) {
        return partnerResponse(id, "Partner", "partner@test.com");
    }

    public static PartnerResponse partnerResponse(UUID id, String name, String email) {
        PartnerResponse response = new PartnerResponse();
        response.setId(id);
        response.setName(name);
        response.setEmail(email);
        return response;
    }
}