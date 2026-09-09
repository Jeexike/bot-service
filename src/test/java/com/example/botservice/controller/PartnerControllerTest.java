package com.example.botservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.botservice.client.PartnerClient;
import com.example.botservice.dto.OrderResponse;
import com.example.botservice.dto.PartnerRequest;
import com.example.botservice.dto.PartnerResponse;
import com.example.botservice.util.SafeResultActions;
import com.example.botservice.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(PartnerController.class)
@EnableRetry
@Import(PartnerController.class)
class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PartnerClient partnerClient;

    @Test
    @DisplayName("POST /partners -> 201")
    void createPartner_ShouldReturnCreated() {

        UUID id = UUID.randomUUID();
        PartnerRequest request = TestDataFactory.partnerRequest();
        PartnerResponse response = TestDataFactory.partnerResponse(id);

        Mockito.when(partnerClient.createPartner(any())).thenReturn(response);

        SafeResultActions result = perform(
                post("/partners").contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Partner"))
                .andExpect(jsonPath("$.email").value("partner@test.com"));
    }

    @Test
    @DisplayName("POST /partners с невалидным телом -> 400")
    void createPartner_InvalidRequest_ShouldReturnBadRequest() {

        PartnerRequest request = TestDataFactory.invalidPartnerRequest();

        SafeResultActions result = perform(
                post("/partners").contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

        result.andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(partnerClient);
    }

    @Test
    @DisplayName("GET /partners/{id}/orders -> 200")
    void getOrdersByPartner_ShouldReturnOrders() {

        UUID partnerId = UUID.randomUUID();
        OrderResponse order = TestDataFactory.orderResponse(UUID.randomUUID(), partnerId);

        Mockito.when(partnerClient.getOrdersByPartnerId(eq(partnerId))).thenReturn(List.of(order));

        SafeResultActions result = perform(get("/partners/{id}/orders", partnerId));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId().toString()))
                .andExpect(jsonPath("$[0].partnerId").value(partnerId.toString()))
                .andExpect(jsonPath("$[0].name").value("Order"));
    }

    @Test
    @DisplayName("DELETE /partners/{id} -> 204")
    void deletePartner_ShouldReturnNoContent() {

        UUID partnerId = UUID.randomUUID();

        Mockito.doNothing().when(partnerClient).deletePartner(partnerId);

        SafeResultActions result = perform(delete("/partners/{id}", partnerId));

        result.andExpect(status().isNoContent());

        Mockito.verify(partnerClient).deletePartner(partnerId);
    }

    @Test
    @DisplayName("GET /partners/{id}/orders с невалидным ID -> 400")
    void getOrdersByPartner_nonUuidId_returnsBadRequest() {

        SafeResultActions result = perform(get("/partners/{id}/orders", "not-a-uuid"));

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /partners/{id} с невалидным ID -> 400")
    void deletePartner_nonUuidId_returnsBadRequest() {

        SafeResultActions result = perform(delete("/partners/{id}", "not-a-uuid"));

        result.andExpect(status().isBadRequest());
    }

    private SafeResultActions perform(MockHttpServletRequestBuilder requestBuilder) {
        try {
            return new SafeResultActions(mockMvc.perform(requestBuilder));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
