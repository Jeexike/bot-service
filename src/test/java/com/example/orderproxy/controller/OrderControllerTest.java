package com.example.orderproxy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.util.SafeResultActions;
import com.example.orderproxy.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderClient orderClient;

    @Test
    @DisplayName("GET /orders/{id} -> 200")
    void getOrder_ShouldReturn200() {

        UUID id = UUID.randomUUID();
        OrderResponse response = TestDataFactory.orderResponse(id);

        when(orderClient.getOrder(id)).thenReturn(response);

        SafeResultActions result = perform(get("/orders/{id}", id));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Order"))
                .andExpect(jsonPath("$.source").value("Moscow"))
                .andExpect(jsonPath("$.destination").value("SPB"))
                .andExpect(jsonPath("$.link").value("https://github.com/Jeexike/order-proxy"));

        verify(orderClient).getOrder(id);
    }

    @Test
    @DisplayName("GET /orders -> 200")
    void getOrders_ShouldReturn200() {

        OrderResponse first = TestDataFactory.orderResponse(UUID.randomUUID());
        OrderResponse second = TestDataFactory.orderResponse(UUID.randomUUID());

        when(orderClient.getOrders()).thenReturn(List.of(first, second));

        SafeResultActions result = perform(get("/orders"));

        result.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        verify(orderClient).getOrders();
    }

    @Test
    @DisplayName("POST /orders -> 201")
    void createOrder_ShouldReturn201() {

        UUID partnerId = UUID.randomUUID();
        OrderRequest request = TestDataFactory.orderRequest(partnerId);
        OrderResponse response = TestDataFactory.orderResponse(UUID.randomUUID(), partnerId);

        when(orderClient.createOrder(any(OrderRequest.class))).thenReturn(response);

        SafeResultActions result =
                perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Order"))
                .andExpect(jsonPath("$.source").value("Moscow"))
                .andExpect(jsonPath("$.destination").value("SPB"))
                .andExpect(jsonPath("$.link").value("https://github.com/Jeexike/order-proxy"))
                .andExpect(jsonPath("$.partnerId").value(partnerId.toString()));

        verify(orderClient).createOrder(any(OrderRequest.class));
    }

    @Test
    @DisplayName("POST /orders невалидное тело -> 400")
    void createOrder_ShouldReturn400_WhenValidationFails() {

        OrderRequest request = TestDataFactory.invalidOrderRequest();

        SafeResultActions result =
                perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(toJson(request)));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("GET /orders/{id} с невалидным ID -> 400")
    void getOrder_nonUuidId_returnsBadRequest() {

        SafeResultActions result = perform(get("/orders/{id}", "not-a-uuid"));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("DELETE /orders/{id} -> 204")
    void deleteOrder_ShouldReturn204() {

        UUID id = UUID.randomUUID();

        SafeResultActions result = perform(delete("/orders/{id}", id));

        result.andExpect(status().isNoContent());

        verify(orderClient).deleteOrder(id);
    }

    @Test
    @DisplayName("DELETE /orders/{id} с невалидным ID -> 400")
    void deleteOrder_nonUuidId_returnsBadRequest() {

        SafeResultActions result = perform(delete("/orders/{id}", "not-a-uuid"));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(orderClient);
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
