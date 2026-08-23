package com.example.orderproxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest
@ActiveProfiles("resilience")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("OrderClient resilience (CB / RL / Retry)")
class OrderClientResilienceTest {

    static HttpServer httpServer;
    static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    static volatile int responseCode = 200;
    static volatile String responseBody = "[]";

    @Autowired
    OrderClient orderClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    RateLimiterRegistry rateLimiterRegistry;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", exchange -> {
            REQUEST_COUNT.incrementAndGet();
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        httpServer.start();
        String base = "http://localhost:" + httpServer.getAddress().getPort();
        registry.add("rest-client.base-url", () -> base);
        registry.add("rest-client.partners-base-url", () -> base);
        registry.add("telegram.bot.enabled", () -> "false");
        registry.add("telegram.bot.token", () -> "");
    }

    @AfterAll
    static void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @BeforeEach
    void resetCircuit() {
        circuitBreakerRegistry.circuitBreaker("orderService").reset();
        REQUEST_COUNT.set(0);
        responseCode = 200;
        responseBody = "[]";
        ensurePermits();
    }

    private void ensurePermits() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1000)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        rateLimiterRegistry.replace("orderService", RateLimiter.of("orderService", config));
    }

    @Test
    @Order(1)
    @DisplayName("4xx → HttpClientErrorException")
    void clientError404_isPropagated() {
        responseCode = 404;
        responseBody = "{\"message\":\"not found\"}";

        assertThatThrownBy(() -> orderClient.getOrder(UUID.randomUUID()))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> assertThat(
                                ((HttpClientErrorException) ex).getStatusCode().value())
                        .isEqualTo(404));
    }

    @Test
    @Order(2)
    @DisplayName("Retry: при 500 несколько HTTP-вызовов (1..3)")
    void retry_onServerError() {
        responseCode = 500;
        responseBody = "error";
        REQUEST_COUNT.set(0);

        try {
            orderClient.getOrder(UUID.randomUUID());
        } catch (Exception ignored) {
        }

        assertThat(REQUEST_COUNT.get()).isBetween(1, 3);
    }

    @Test
    @Order(3)
    @DisplayName("CircuitBreaker: серия 5xx → failed calls > 0")
    void circuitBreaker_recordsFailures() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("orderService");
        int failedBefore = cb.getMetrics().getNumberOfFailedCalls();
        responseCode = 500;
        responseBody = "x";

        for (int i = 0; i < 6; i++) {
            try {
                orderClient.getOrder(UUID.randomUUID());
            } catch (Exception ignored) {
            }
        }

        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isGreaterThan(failedBefore);
    }

    @Test
    @Order(4)
    @DisplayName("Circuit OPEN: getOrders → empty list, без HTTP")
    void whenOpen_getOrdersReturnsEmptyStub() {
        circuitBreakerRegistry.circuitBreaker("orderService").transitionToOpenState();
        REQUEST_COUNT.set(0);

        List<?> result = orderClient.getOrders();

        assertThat(result).isEmpty();
        assertThat(REQUEST_COUNT.get()).isZero();
    }

    @Test
    @Order(5)
    @DisplayName("RateLimiter: нет permits → RequestNotPermitted")
    void rateLimiter_blocksWhenNoPermitsLeft() {
        RateLimiterConfig tight = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ZERO)
                .build();
        RateLimiter tightRl = RateLimiter.of("orderService", tight);
        rateLimiterRegistry.replace("orderService", tightRl);

        assertThat(tightRl.acquirePermission()).isTrue();
        assertThatThrownBy(() -> orderClient.getOrders()).isInstanceOf(RequestNotPermitted.class);
    }
}
