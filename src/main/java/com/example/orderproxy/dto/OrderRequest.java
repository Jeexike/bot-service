package com.example.orderproxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank(message = "Source cannot be blank")
    @Size(min = 2, max = 255)
    private String source;

    @NotBlank(message = "Destination cannot be blank")
    @Size(min = 2, max = 255)
    private String destination;

    @Schema(description = "Partner ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID partnerId;
}