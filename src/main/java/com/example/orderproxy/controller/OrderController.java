package com.example.orderproxy.controller;

import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Операции с заказами")
public class OrderController {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public OrderController(
            @Qualifier("restTemplate") RestTemplate restTemplate,
            @Value("${base.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заказ по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse getOrder( @Parameter(description = "ID заказа") @PathVariable UUID id) {
        return restTemplate.getForObject(baseUrl + "/" + id, OrderResponse.class);
    }

    @GetMapping
    @Operation(summary = "Получить список всех заказов")
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public List<OrderResponse> getOrders() {
        OrderResponse[] orders = restTemplate.getForObject(baseUrl, OrderResponse[].class);
        if (orders == null) {
            throw new IllegalStateException("Внешний сервис вернул пустое тело ответа");
        }
        return Arrays.asList(orders);
    }

    @PostMapping
    @Operation(summary = "Создать новый заказ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ создан"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return restTemplate.postForObject(baseUrl, orderRequest, OrderResponse.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующий заказ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ обновлён"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public OrderResponse updateOrder(@Parameter(description = "ID заказа") @PathVariable UUID id, @Valid @RequestBody OrderRequest orderRequest) {
        return restTemplate.exchange(baseUrl + "/" + id, HttpMethod.PUT, new HttpEntity<>(orderRequest), OrderResponse.class).getBody();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить заказ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ удалён"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${rest-client.max-attempts}",
            backoff = @Backoff(delayExpression = "${rest-client.backoff-delay}")
    )
    public void deleteOrder(@Parameter(description = "ID заказа") @PathVariable UUID id) {
        restTemplate.delete(baseUrl + "/" + id);
    }
}
