package com.example.botservice.telegram;

import com.example.botservice.telegram.dto.TelegramDtos.GetUpdatesResponse;
import com.example.botservice.telegram.dto.TelegramDtos.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramLongPollingBot {

    private final TelegramProperties properties;
    private final TelegramApiClient telegramApi;
    private final TelegramCommandDispatcher dispatcher;

    private long offset;

    @Scheduled(fixedDelayString = "${telegram.bot.schedule-delay-ms:500}")
    public void poll() {
        if (!properties.isEnabled() || !telegramApi.isConfigured()) {
            return;
        }
        try {
            GetUpdatesResponse response = telegramApi.getUpdates(offset, properties.getPollTimeoutSeconds());
            if (response == null || !response.isOk() || response.getResult() == null) {
                return;
            }
            for (Update update : response.getResult()) {
                offset = update.getUpdateId() + 1;
                try {
                    dispatcher.dispatch(update.getMessage());
                } catch (Exception e) {
                    log.error("Failed to handle update {}: {}", update.getUpdateId(), e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Telegram poll error: {}", e.toString());
        }
    }
}
