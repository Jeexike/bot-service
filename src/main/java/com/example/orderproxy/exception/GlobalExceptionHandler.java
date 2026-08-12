package com.example.orderproxy.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        log.warn("Field validation failed: {}", fieldErrors);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        String message = "Parameter '%s' has invalid value".formatted(ex.getName());
        log.warn("{}: {}", message, ex.getValue());
        return buildError(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleUpstreamError(RestClientResponseException ex) {

        log.warn(
                "Order service returned an error: {} {} - {}",
                ex.getStatusCode().value(),
                ex.getStatusText(),
                ex.getResponseBodyAsString());

        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ResourceAccessException ex) {

        log.error("Order service is unavailable", ex);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "Order service is unavailable", null);
    }

    @ExceptionHandler(OrderServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleOrderServiceUnavailable(OrderServiceUnavailableException ex) {

        log.error("Order service call failed, circuit breaker fallback engaged", ex);
        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE, "Order service is temporarily unavailable, try again later", null);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(CallNotPermittedException ex) {

        log.warn("Circuit breaker '{}' is OPEN, request rejected fast", ex.getCausingCircuitBreakerName());
        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE, "Order service is temporarily unavailable, try again later", null);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RequestNotPermitted ex) {

        log.warn("Rate limiter rejected request: {}", ex.getMessage());
        return buildError(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, try again later", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {

        log.error("Unexpected error occurred", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message, Object details) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        if (details != null) {
            body.put("details", details);
        }

        return ResponseEntity.status(status).body(body);
    }
}
