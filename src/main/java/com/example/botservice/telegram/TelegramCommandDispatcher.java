package com.example.botservice.telegram;

import com.example.botservice.telegram.command.BotCommand;
import com.example.botservice.telegram.dto.TelegramDtos.Message;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCommandDispatcher {

    private final List<BotCommand> commands;
    private final TelegramApiClient telegramApi;

    public void dispatch(Message message) {
        if (message == null || message.getChat() == null) {
            return;
        }
        long chatId = message.getChat().getId();
        String text = message.getText() == null ? "" : message.getText().trim();
        if (text.isEmpty()) {
            telegramApi.sendMessage(chatId, "Пришлите текстовую команду. /help");
            return;
        }

        List<BotCommand> ordered = commands.stream()
                .sorted(Comparator.comparingInt(BotCommand::getOrder))
                .toList();

        for (BotCommand command : ordered) {
            if (command.supports(chatId, text)) {
                try {
                    command.handle(chatId, text);
                } catch (Exception e) {
                    log.error("Command /{} failed chatId={}: {}", command.name(), chatId, e.toString());
                    telegramApi.sendMessage(chatId, "❌ Внутренняя ошибка: " + e.getMessage());
                }
                return;
            }
        }
        telegramApi.sendMessage(chatId, "Неизвестная команда. /help");
    }
}
