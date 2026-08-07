package com.example.orderproxy.kafka;

import com.example.orderproxy.kafka.dto.OrderLinkChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLinkChangedConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${tracking.outbox.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        try {
            OrderLinkChangedEvent event = objectMapper.readValue(payload, OrderLinkChangedEvent.class);
            log.info(
                    "Received order link change: orderId={}, link={}, changedFields={}, detectedAt={}",
                    event.orderId(),
                    event.link(),
                    event.changedFields(),
                    event.detectedAt());
        } catch (JacksonException e) {
            log.error("Failed to process order link change message: {}", payload, e);
        } catch (Exception e) {
            log.error("Unexpected error while processing order link change message: {}", payload, e);
        }
    }
}
