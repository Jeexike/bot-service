package com.example.orderproxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderproxy.exception.OrderServiceUnavailableException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@DisplayName("ClientResilienceSupport")
class ClientResilienceSupportTest {

    @Test
    @DisplayName("RequestNotPermitted пробрасывается")
    void rethrowsRequestNotPermitted() {
        RateLimiter rl = RateLimiter.of(
                "test",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(Duration.ofSeconds(60))
                        .timeoutDuration(Duration.ZERO)
                        .build());
        assertThat(rl.acquirePermission()).isTrue();
        RequestNotPermitted denied = RequestNotPermitted.createRequestNotPermitted(rl);

        assertThatThrownBy(() -> ClientResilienceSupport.rethrowBusinessOrRateLimit(denied))
                .isSameAs(denied);
    }

    @Test
    @DisplayName("HttpClientErrorException (4xx) пробрасывается")
    void rethrowsHttpClientError() {
        HttpClientErrorException notFound =
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);

        assertThatThrownBy(() -> ClientResilienceSupport.rethrowBusinessOrRateLimit(notFound))
                .isInstanceOf(HttpClientErrorException.class)
                .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("прочие ошибки не пробрасываются")
    void otherExceptions_areNotRethrown() {
        ClientResilienceSupport.rethrowBusinessOrRateLimit(new RuntimeException("connection reset"));
    }

    @Test
    @DisplayName("unavailable оборачивает причину")
    void unavailableWrapsCause() {
        RuntimeException cause = new RuntimeException("down");
        OrderServiceUnavailableException ex =
                ClientResilienceSupport.unavailable("OrderClient", "getOrder", cause);

        assertThat(ex).hasMessageContaining("unavailable").hasCause(cause);
    }
}