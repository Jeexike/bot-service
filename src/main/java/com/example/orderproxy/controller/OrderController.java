package com.example.orderproxy.controller;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Операции с заказами")
public class OrderController implements OrderApi {

    private final OrderClient orderClient;

    @Override
    public OrderResponse getOrder(@Parameter(description = "ID заказа") @PathVariable UUID id) {
        return orderClient.getOrder(id);
    }

    @Override
    public List<OrderResponse> getOrders() {
        return orderClient.getOrders();
    }

    @Override
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return orderClient.createOrder(orderRequest);
    }

    @Override
    public OrderResponse updateOrder(
            @Parameter(description = "ID заказа") @PathVariable UUID id,
            @Valid @RequestBody OrderRequest orderRequest) {
        return orderClient.updateOrder(id, orderRequest);
    }

    @Override
    public void deleteOrder(@Parameter(description = "ID заказа") @PathVariable UUID id) {
        orderClient.deleteOrder(id);
    }
}
