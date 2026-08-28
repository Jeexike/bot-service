package com.example.botservice.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;

@Data
@JsonPropertyOrder({"id", "name", "source", "destination", "partnerId", "createdAt", "updatedAt"})
@Schema(description = "Ответ с данными заказа")
public class OrderResponse {

    @Schema(description = "Уникальный идентификатор заказа", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Название заказа", example = "Order #1")
    private String name;

    @Schema(description = "Точка отправления", example = "Москва")
    private String source;

    @Schema(description = "Точка назначения", example = "Санкт-Петербург")
    private String destination;

    @Schema(
            description = "Ссылка на GitHub-репозиторий, связанный с заказом",
            example = "https://github.com/Jeexike/order-proxy")
    private String link;

    @Schema(description = "ID партнера")
    private UUID partnerId;

    @Schema(description = "Время создания")
    private Timestamp createdAt;

    @Schema(description = "Время последнего обновления")
    private Timestamp updatedAt;
}
