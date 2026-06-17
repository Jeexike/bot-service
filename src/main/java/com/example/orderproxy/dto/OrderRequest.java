package com.example.orderproxy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
}