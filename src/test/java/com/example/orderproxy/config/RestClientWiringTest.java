package com.example.orderproxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.client.PartnerClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.PartnerRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = {AppConfig.class, PartnerClient.class, OrderClient.class})
class RestClientWiringTest {

    private static HttpServer ordersServer;
    private static HttpServer partnersServer;

    private static final AtomicReference<String> lastOrdersPath = new AtomicReference<>();
    private static final AtomicReference<String> lastPartnersPath = new AtomicReference<>();

    static {
        try {
            ordersServer = startStubServer(lastOrdersPath, "{}");
            partnersServer = startStubServer(lastPartnersPath, """
                    {"id":"%s","name":"Name","email":"a@b.com"}
                    """.formatted(UUID.randomUUID()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start stub HTTP servers", e);
        }
    }

    @Autowired
    private PartnerClient partnerClient;

    @Autowired
    private OrderClient orderClient;

    @AfterAll
    static void stopStubServers() {
        ordersServer.stop(0);
        partnersServer.stop(0);
    }

    @DynamicPropertySource
    static void registerBaseUrls(DynamicPropertyRegistry registry) {
        registry.add(
                "rest-client.base-url",
                () -> "http://localhost:" + ordersServer.getAddress().getPort() + "/orders");
        registry.add(
                "rest-client.partners-base-url",
                () -> "http://localhost:" + partnersServer.getAddress().getPort() + "/partners");
        registry.add("rest-client.connect-timeout", () -> 3000);
        registry.add("rest-client.read-timeout", () -> 3000);
    }

    @Test
    void partnerClient_shouldOnlyHitPartnersServer() {
        lastOrdersPath.set(null);
        lastPartnersPath.set(null);

        PartnerRequest request = new PartnerRequest();
        request.setName("Name");
        request.setEmail("a@b.com");

        partnerClient.createPartner(request);

        assertThat(lastPartnersPath.get()).isEqualTo("/partners");
        assertThat(lastOrdersPath.get()).isNull();
    }

    @Test
    void orderClient_shouldOnlyHitOrdersServer() {
        lastOrdersPath.set(null);
        lastPartnersPath.set(null);

        OrderRequest request = new OrderRequest();
        request.setName("Order");
        request.setSource("A");
        request.setDestination("B");
        request.setLink("https://github.com/example/repo");
        request.setPartnerId(UUID.randomUUID());

        orderClient.createOrder(request);

        assertThat(lastOrdersPath.get()).isEqualTo("/orders");
        assertThat(lastPartnersPath.get()).isNull();
    }

    private static HttpServer startStubServer(AtomicReference<String> capturedPath, String jsonResponse)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            byte[] body = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }
}
