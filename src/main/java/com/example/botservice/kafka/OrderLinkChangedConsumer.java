package com.example.botservice.kafka;

import com.example.botservice.client.OrderClient;
import com.example.botservice.dto.OrderResponse;
import com.example.botservice.kafka.dto.OrderLinkChangedEvent;
import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramMessageFormatter;
import com.example.botservice.telegram.TelegramProperties;
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
    private final TelegramApiClient telegramApi;
    private final TelegramMessageFormatter formatter;
    private final TelegramProperties telegramProperties;
    private final OrderClient orderClient;

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
            notifyTelegram(event);
        } catch (JacksonException e) {
            log.error("Failed to process order link change message: {}", payload, e);
        } catch (Exception e) {
            log.error("Unexpected error while processing order link change message: {}", payload, e);
        }
    }

    private void notifyTelegram(OrderLinkChangedEvent event) {
        if (!telegramProperties.isEnabled() || !telegramApi.isConfigured()) {
            return;
        }
        Long chatId = telegramProperties.getNotificationsChatId();
        if (chatId == null) {
            log.debug("telegram.bot.notifications-chat-id is not set, skipping notification");
            return;
        }

        OrderResponse order = null;
        try {
            order = orderClient.getOrder(event.orderId());
        } catch (Exception e) {
            log.warn("Could not fetch order {} to enrich Telegram notification: {}", event.orderId(), e.toString());
        }

        telegramApi.sendMessage(chatId, formatter.orderLinkChanged(event, order));
    }
}