package com.example.orderproxy.controller;

import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface PartnerApi {

    @PostMapping
    @Operation(summary = "Создать партнера")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Партнер создан"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    PartnerResponse createPartner(@Valid @RequestBody PartnerRequest request);

    @GetMapping("/{partnerId}/orders")
    @Operation(summary = "Получить все заказы партнера")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список заказов получен"),
        @ApiResponse(responseCode = "404", description = "Партнер не найден")
    })
    List<OrderResponse> getOrdersByPartnerId(@Parameter(description = "ID партнера") @PathVariable UUID partnerId);

    @DeleteMapping("/{partnerId}")
    @Operation(summary = "Удалить партнера (каскадно удалятся его заказы)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Партнер удалён"),
        @ApiResponse(responseCode = "404", description = "Партнер не найден")
    })
    void deletePartner(@Parameter(description = "ID партнера") @PathVariable UUID partnerId);
}
