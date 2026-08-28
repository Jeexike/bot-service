package com.example.botservice.exception;

public class OrderServiceUnavailableException extends RuntimeException {

    public OrderServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
