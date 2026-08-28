package com.example.botservice.telegram.command;

import com.example.botservice.client.OrderClient;
import com.example.botservice.telegram.TelegramApiClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class OrderDeleteCommand implements BotCommand {

    private final OrderClient orderClient;
    private final TelegramApiClient telegramApi;

    @Override
    public String name() {
        return "order_delete";
    }

    @Override
    public String description() {
        return "удалить заказ: /order_delete &lt;uuid&gt;";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name());
    }

    @Override
    public void handle(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            telegramApi.sendMessage(chatId, "Использование: /order_delete &lt;uuid&gt;");
            return;
        }
        try {
            UUID id = UUID.fromString(parts[1].trim());
            orderClient.deleteOrder(id);
            telegramApi.sendMessage(chatId, "✅ Заказ <code>" + id + "</code> удалён");
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
