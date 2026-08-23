package com.example.orderproxy.telegram;

import com.example.orderproxy.telegram.dto.TelegramDtos.GetUpdatesResponse;
import com.example.orderproxy.telegram.dto.TelegramDtos.Update;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramLongPollingBot {

    private final TelegramProperties properties;
    private final TelegramApiClient telegramApi;
    private final OrderTelegramHandler handler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong offset = new AtomicLong(0);
    private Thread worker;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled() || !telegramApi.isConfigured()) {
            log.info("Telegram bot is disabled (set telegram.bot.token and telegram.bot.enabled=true)");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = new Thread(this::pollLoop, "telegram-bot-poller");
        worker.setDaemon(true);
        worker.start();
        log.info("Telegram bot long-polling started (user=@{})", properties.getUsername());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void pollLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                GetUpdatesResponse response = telegramApi.getUpdates(offset.get(), properties.getPollTimeoutSeconds());
                if (response == null || !response.isOk() || response.getResult() == null) {
                    continue;
                }
                List<Update> updates = response.getResult();
                for (Update update : updates) {
                    offset.set(update.getUpdateId() + 1);
                    try {
                        handler.handle(update.getMessage());
                    } catch (Exception e) {
                        log.error("Failed to handle update {}: {}", update.getUpdateId(), e.toString());
                    }
                }
            } catch (Exception e) {
                if (!running.get()) {
                    break;
                }
                log.warn("Telegram poll error: {}", e.toString());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Telegram bot long-polling stopped");
    }
}
