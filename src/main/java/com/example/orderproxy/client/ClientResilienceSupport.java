package com.example.orderproxy.client;

import com.example.orderproxy.exception.OrderServiceUnavailableException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@UtilityClass
public class ClientResilienceSupport {

    public static void rethrowBusinessOrRateLimit(Throwable ex) {
        if (ex instanceof RequestNotPermitted rnp) {
            throw rnp;
        }
        if (ex instanceof HttpClientErrorException hce) {
            throw hce;
        }
    }

    public static OrderServiceUnavailableException unavailable(String client, String operation, Throwable ex) {
        log.error("Resilience fallback triggered for {}.{}: {}", client, operation, ex.toString());
        return new OrderServiceUnavailableException("Order service is currently unavailable", ex);
    }
}
