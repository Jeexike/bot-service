package com.example.botservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(description = "Partner response")
public class PartnerResponse {

    @Schema(description = "Partner ID")
    private UUID id;

    @Schema(description = "Partner name")
    private String name;

    @Schema(description = "Partner email")
    private String email;

    @Schema(description = "Created at")
    private Timestamp createdAt;

    @Schema(description = "Updated at")
    private Timestamp updatedAt;
}
