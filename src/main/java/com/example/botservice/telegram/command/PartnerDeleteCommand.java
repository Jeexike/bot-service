package com.example.botservice.telegram.command;

import com.example.botservice.client.PartnerClient;
import com.example.botservice.telegram.TelegramApiClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PartnerDeleteCommand implements BotCommand {

    private final PartnerClient partnerClient;
    private final TelegramApiClient telegramApi;

    @Override
    public String name() {
        return "partner_delete";
    }

    @Override
    public String description() {
        return "удалить партнёра: /partner_delete &lt;uuid&gt;";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name());
    }

    @Override
    public void handle(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            telegramApi.sendMessage(chatId, "Использование: /partner_delete &lt;uuid&gt;");
            return;
        }
        try {
            UUID id = UUID.fromString(parts[1].trim());
            partnerClient.deletePartner(id);
            telegramApi.sendMessage(chatId, "✅ Партнёр <code>" + id + "</code> удалён\n(заказы удаляются каскадно)");
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный UUID");
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "❌ Ошибка (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис недоступен: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
