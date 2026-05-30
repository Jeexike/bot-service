package com.example.orderproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private UUID id;
    private String name;
    private String source;
    private String destination;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}

