package com.example.botservice.telegram.command;

import com.example.botservice.telegram.TelegramApiClient;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements BotCommand {

    private final List<BotCommand> commands;
    private final TelegramApiClient telegramApi;

    public HelpCommand(@Lazy List<BotCommand> commands, TelegramApiClient telegramApi) {
        this.commands = commands;
        this.telegramApi = telegramApi;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "список всех команд";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return startsWithCommand(text, "help");
    }

    @Override
    public void handle(long chatId, String text) {
        StringBuilder sb = new StringBuilder("<b>Доступные команды</b>\n\n");
        commands.stream().sorted(Comparator.comparing(BotCommand::name)).forEach(c -> sb.append("/")
                .append(c.name())
                .append(" - ")
                .append(c.description())
                .append("\n"));
        telegramApi.sendMessage(chatId, sb.toString());
    }

    static boolean startsWithCommand(String text, String name) {
        String lower = text.toLowerCase();
        return lower.equals("/" + name) || lower.startsWith("/" + name + " ") || lower.startsWith("/" + name + "@");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
