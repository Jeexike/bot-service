package com.example.botservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

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

    @NotNull(message = "Link cannot be blank")
    @Pattern(
            regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$",
            message = "Link must be a valid GitHub repository URL, e.g. https://github.com/owner/repo")
    @Schema(
            description = "Ссылка на GitHub-репозиторий, связанный с заказом",
            example = "https://github.com/Jeexike/order-proxy")
    private String link;

    @NotNull(message = "Partner ID cannot be null")
    @Schema(description = "Partner ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID partnerId;
}
