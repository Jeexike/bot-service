package com.example.orderproxy.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@JsonPropertyOrder({"id", "name", "source", "destination", "createdAt", "updatedAt"})
public class OrderResponse {
    private UUID id;
    private String name;
    private String source;
    private String destination;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
