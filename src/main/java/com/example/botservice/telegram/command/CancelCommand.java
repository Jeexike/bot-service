package com.example.botservice.telegram.command;

import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements BotCommand {

    private final TelegramApiClient telegramApi;
    private final TelegramSessionStore sessions;

    @Override
    public String name() {
        return "cancel";
    }

    @Override
    public String description() {
        return "отменить текущий диалог";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, "cancel");
    }

    @Override
    public void handle(long chatId, String text) {
        sessions.clearAll(chatId);
        telegramApi.sendMessage(chatId, "Диалог отменён.");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
