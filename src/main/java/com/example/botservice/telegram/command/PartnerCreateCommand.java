package com.example.botservice.telegram.command;

import com.example.botservice.client.PartnerClient;
import com.example.botservice.dto.PartnerRequest;
import com.example.botservice.dto.PartnerResponse;
import com.example.botservice.telegram.CreatePartnerSession;
import com.example.botservice.telegram.CreatePartnerSession.Step;
import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramMessageFormatter;
import com.example.botservice.telegram.TelegramSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerCreateCommand implements BotCommand {

    private final PartnerClient partnerClient;
    private final TelegramApiClient telegramApi;
    private final TelegramSessionStore sessions;
    private final TelegramMessageFormatter formatter;

    @Override
    public String name() {
        return "partner_create";
    }

    @Override
    public String description() {
        return "создать партнёра (пошагово)";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name()) || sessions.getPartnerSession(chatId) != null;
    }

    @Override
    public void handle(long chatId, String text) {
        CreatePartnerSession session = sessions.getPartnerSession(chatId);
        if (session == null) {
            sessions.putPartnerSession(chatId, new CreatePartnerSession());
            telegramApi.sendMessage(chatId, "Создание партнёра.\nВведите <b>имя</b> (2–255):\n/cancel - отмена");
            return;
        }
        try {
            switch (session.getStep()) {
                case NAME -> {
                    if (text.length() < 2 || text.length() > 255) {
                        telegramApi.sendMessage(chatId, "Имя: 2–255 символов. Ещё раз:");
                        return;
                    }
                    session.setName(text);
                    session.setStep(Step.EMAIL);
                    telegramApi.sendMessage(chatId, "Введите <b>email</b> или <code>-</code>, чтобы пропустить:");
                }
                case EMAIL -> {
                    if (!"-".equals(text.trim()) && !text.isBlank()) {
                        session.setEmail(text.trim());
                    }
                    create(chatId, session);
                    sessions.clearPartnerSession(chatId);
                }
            }
        } catch (Exception e) {
            log.error("partner_create dialog chatId={}: {}", chatId, e.toString());
            sessions.clearPartnerSession(chatId);
            telegramApi.sendMessage(chatId, "Ошибка: " + e.getMessage() + "\n/partner_create");
        }
    }

    private void create(long chatId, CreatePartnerSession session) {
        PartnerRequest request = new PartnerRequest();
        request.setName(session.getName());
        request.setEmail(session.getEmail());
        try {
            PartnerResponse created = partnerClient.createPartner(request);
            telegramApi.sendMessage(chatId, formatter.partnerCreated(created));
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "❌ Ошибка (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис недоступен: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
