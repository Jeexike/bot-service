package com.example.orderproxy.controller;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Операции с заказами")
public class OrderController {

    private final OrderClient orderClient;

    @GetMapping("/{id}")
    @Operation(summary = "Получить заказ по ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заказ найден"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @Retryable(
            retryFor = WebClientRequestException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}"))
    public OrderResponse getOrder(@Parameter(description = "ID заказа") @PathVariable UUID id) {
        return orderClient.getOrder(id);
    }

    @GetMapping
    @Operation(summary = "Получить список всех заказов")
    @Retryable(
            retryFor = WebClientRequestException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}"))
    public List<OrderResponse> getOrders() {
        return orderClient.getOrders();
    }

    @PostMapping
    @Operation(summary = "Создать новый заказ")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заказ создан"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    @Retryable(
            retryFor = WebClientRequestException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}"))
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return orderClient.createOrder(orderRequest);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующий заказ")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заказ обновлён"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    @Retryable(
            retryFor = WebClientRequestException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}"))
    public OrderResponse updateOrder(
            @Parameter(description = "ID заказа") @PathVariable UUID id,
            @Valid @RequestBody OrderRequest orderRequest) {
        return orderClient.updateOrder(id, orderRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить заказ")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заказ удалён"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @Retryable(
            retryFor = WebClientRequestException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}"))
    public void deleteOrder(@Parameter(description = "ID заказа") @PathVariable UUID id) {
        orderClient.deleteOrder(id);
    }
}
