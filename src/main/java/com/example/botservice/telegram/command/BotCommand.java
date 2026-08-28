package com.example.botservice.telegram.command;

import org.springframework.core.Ordered;

public interface BotCommand extends Ordered {

    String name();

    String description();

    boolean supports(long chatId, String text);

    void handle(long chatId, String text);

    @Override
    default int getOrder() {
        return 100;
    }
}
