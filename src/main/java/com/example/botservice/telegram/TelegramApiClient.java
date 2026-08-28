package com.example.botservice.telegram;

import com.example.botservice.telegram.dto.TelegramDtos.GetUpdatesResponse;
import com.example.botservice.telegram.dto.TelegramDtos.SendMessageResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TelegramApiClient {

    private final RestClient telegramRestClient;
    private final TelegramProperties properties;

    public TelegramApiClient(TelegramProperties properties) {
        this.properties = properties;
        this.telegramRestClient =
                RestClient.builder().baseUrl("https://api.telegram.org").build();
    }

    public boolean isConfigured() {
        return properties.getToken() != null && !properties.getToken().isBlank();
    }

    public GetUpdatesResponse getUpdates(long offset, int timeoutSeconds) {
        String path = "/bot"
                + properties.getToken()
                + "/getUpdates?offset="
                + offset
                + "&timeout="
                + timeoutSeconds
                + "&allowed_updates=message";

        return telegramRestClient.get().uri(path).retrieve().body(GetUpdatesResponse.class);
    }

    public void sendMessage(long chatId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");

        String path = "/bot" + properties.getToken() + "/sendMessage";
        try {
            telegramRestClient.post().uri(path).body(body).retrieve().body(SendMessageResponse.class);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId={}: {}", chatId, e.toString());
        }
    }
}
