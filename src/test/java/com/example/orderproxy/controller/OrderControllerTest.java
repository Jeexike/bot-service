package com.example.orderproxy.controller;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderClient orderClient;

    @Test
    @DisplayName("GET /orders/{id} -> 200")
    void getOrder_ShouldReturn200() throws Exception {

        UUID id = UUID.randomUUID();

        OrderResponse response = new OrderResponse();
        response.setId(id);
        response.setName("Order");
        response.setSource("Moscow");
        response.setDestination("SPB");

        when(orderClient.getOrder(id)).thenReturn(response);

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Order"))
                .andExpect(jsonPath("$.source").value("Moscow"))
                .andExpect(jsonPath("$.destination").value("SPB"));

        verify(orderClient).getOrder(id);
    }

    @Test
    @DisplayName("GET /orders -> 200")
    void getOrders_ShouldReturn200() throws Exception {

        OrderResponse first = new OrderResponse();
        first.setId(UUID.randomUUID());
        first.setName("First");
        first.setSource("Moscow");
        first.setDestination("SPB");

        OrderResponse second = new OrderResponse();
        second.setId(UUID.randomUUID());
        second.setName("Second");
        second.setSource("Kazan");
        second.setDestination("Omsk");

        when(orderClient.getOrders())
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(orderClient).getOrders();
    }

    @Test
    @DisplayName("POST /orders -> 201")
    void createOrder_ShouldReturn201() throws Exception {

        UUID partnerId = UUID.randomUUID();

        OrderRequest request = new OrderRequest();
        request.setName("Order");
        request.setSource("Moscow");
        request.setDestination("SPB");
        request.setPartnerId(partnerId);

        OrderResponse response = new OrderResponse();
        response.setId(UUID.randomUUID());
        response.setName("Order");
        response.setSource("Moscow");
        response.setDestination("SPB");
        response.setPartnerId(partnerId);

        when(orderClient.createOrder(any(OrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Order"))
                .andExpect(jsonPath("$.source").value("Moscow"))
                .andExpect(jsonPath("$.destination").value("SPB"))
                .andExpect(jsonPath("$.partnerId").value(partnerId.toString()));

        verify(orderClient).createOrder(any(OrderRequest.class));
    }

    @Test
    @DisplayName("POST /orders невалидное тело -> 400")
    void createOrder_ShouldReturn400_WhenValidationFails() throws Exception {

        OrderRequest request = new OrderRequest();
        request.setName("");
        request.setSource("A");
        request.setDestination("B");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("GET /orders/{id} с невалидным ID -> 400")
    void getOrder_nonUuidId_returnsBadRequest() throws Exception {

        mockMvc.perform(get("/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("DELETE /orders/{id} -> 204")
    void deleteOrder_ShouldReturn204() throws Exception {

        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/orders/{id}", id))
                .andExpect(status().isNoContent());

        verify(orderClient).deleteOrder(id);
    }

    @Test
    @DisplayName("DELETE /orders/{id} с невалидным ID -> 400")
    void deleteOrder_nonUuidId_returnsBadRequest() throws Exception {

        mockMvc.perform(delete("/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
    }
}