package com.example.orderproxy.controller;

import com.example.orderproxy.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public OrderController(
            @Qualifier("restTemplate") RestTemplate restTemplate,
            @Value("${base.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable UUID id) {
        return restTemplate.getForObject(baseUrl + "/" + id, Order.class);
    }

    @GetMapping
    public List<Order> getOrders() {
        Order[] orders = restTemplate.getForObject(baseUrl, Order[].class);
        if (orders == null) {
            throw new IllegalStateException("Внешний сервис вернул пустое тело ответа");
        }
        return Arrays.asList(orders);
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return restTemplate.postForObject(baseUrl, order, Order.class);
    }

    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable UUID id, @RequestBody Order order) {
        return restTemplate.exchange(baseUrl + "/" + id, HttpMethod.PUT, new HttpEntity<>(order), Order.class).getBody();
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable UUID id) {
        restTemplate.delete(baseUrl + "/" + id);
    }
}
