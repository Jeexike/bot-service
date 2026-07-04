package com.example.orderproxy.controller;

import com.example.orderproxy.client.PartnerClient;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void createPartner_ShouldReturnCreated() throws Exception {

        UUID id = UUID.randomUUID();

        PartnerRequest request = new PartnerRequest();
        request.setName("Partner");
        request.setEmail("partner@test.com");

        PartnerResponse response = new PartnerResponse();
        response.setId(id);
        response.setName("Partner");
        response.setEmail("partner@test.com");

        Mockito.when(partnerClient.createPartner(any()))
                .thenReturn(response);

        mockMvc.perform(post("/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Partner"))
                .andExpect(jsonPath("$.email").value("partner@test.com"));
    }

    @Test
    @DisplayName("POST /partners с невалидным телом -> 400")
    void createPartner_InvalidRequest_ShouldReturnBadRequest() throws Exception {

        PartnerRequest request = new PartnerRequest();
        request.setName("");
        request.setEmail("not-email");

        mockMvc.perform(post("/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(partnerClient);
    }

    @Test
    @DisplayName("GET /partners/{id}/orders -> 200")
    void getOrdersByPartner_ShouldReturnOrders() throws Exception {

        UUID partnerId = UUID.randomUUID();

        OrderResponse order = new OrderResponse();
        order.setId(UUID.randomUUID());
        order.setName("Order");
        order.setSource("A");
        order.setDestination("B");
        order.setPartnerId(partnerId);

        Mockito.when(partnerClient.getOrdersByPartnerId(eq(partnerId)))
                .thenReturn(List.of(order));

        mockMvc.perform(get("/partners/{id}/orders", partnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId().toString()))
                .andExpect(jsonPath("$[0].partnerId").value(partnerId.toString()))
                .andExpect(jsonPath("$[0].name").value("Order"));
    }

    @Test
    @DisplayName("DELETE /partners/{id} -> 204")
    void deletePartner_ShouldReturnNoContent() throws Exception {

        UUID partnerId = UUID.randomUUID();

        Mockito.doNothing()
                .when(partnerClient)
                .deletePartner(partnerId);

        mockMvc.perform(delete("/partners/{id}", partnerId))
                .andExpect(status().isNoContent());

        Mockito.verify(partnerClient)
                .deletePartner(partnerId);
    }

    @Test
    @DisplayName("GET /partners/{id}/orders с невалидным ID -> 400")
    void getOrdersByPartner_nonUuidId_returnsBadRequest() throws Exception {

        mockMvc.perform(get("/partners/{id}/orders", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /partners/{id} с невалидным ID -> 400")
    void deletePartner_nonUuidId_returnsBadRequest() throws Exception {

        mockMvc.perform(delete("/partners/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}