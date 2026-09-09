package com.example.botservice.telegram.command;

import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements BotCommand {

    private final TelegramApiClient telegramApi;
    private final TelegramSessionStore sessions;

    @Override
    public String name() {
        return "start";
    }

    @Override
    public String description() {
        return "приветствие";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, "start");
    }

    @Override
    public void handle(long chatId, String text) {
        sessions.clearAll(chatId);
        telegramApi.sendMessage(chatId, "Привет! Я бот для работы с заказами и партнёрами.\n" + "Список команд: /help");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
