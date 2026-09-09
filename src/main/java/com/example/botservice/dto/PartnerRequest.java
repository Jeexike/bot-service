package com.example.botservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PartnerRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255)
    private String name;

    @Email(message = "Invalid email format")
    private String email;
}
