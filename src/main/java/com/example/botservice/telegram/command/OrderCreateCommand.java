package com.example.botservice.telegram.command;

import com.example.botservice.client.OrderClient;
import com.example.botservice.dto.OrderRequest;
import com.example.botservice.dto.OrderResponse;
import com.example.botservice.telegram.CreateOrderSession;
import com.example.botservice.telegram.CreateOrderSession.Step;
import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramMessageFormatter;
import com.example.botservice.telegram.TelegramSessionStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateCommand implements BotCommand {

    private final OrderClient orderClient;
    private final TelegramApiClient telegramApi;
    private final TelegramSessionStore sessions;
    private final TelegramMessageFormatter formatter;

    @Override
    public String name() {
        return "order_create";
    }

    @Override
    public String description() {
        return "создать заказ (пошагово)";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name()) || sessions.getOrderSession(chatId) != null;
    }

    @Override
    public void handle(long chatId, String text) {
        CreateOrderSession session = sessions.getOrderSession(chatId);
        if (session == null) {
            sessions.putOrderSession(chatId, new CreateOrderSession());
            telegramApi.sendMessage(
                    chatId,
                    "Создание заказа.\nНужен partnerId - /partner_list\n\n"
                            + "Введите <b>название</b>:\n/cancel - отмена");
            return;
        }
        try {
            switch (session.getStep()) {
                case NAME -> {
                    session.setName(text);
                    session.setStep(Step.SOURCE);
                    telegramApi.sendMessage(chatId, "Город <b>отправки</b> (source):");
                }
                case SOURCE -> {
                    session.setSource(text);
                    session.setStep(Step.DESTINATION);
                    telegramApi.sendMessage(chatId, "Город <b>доставки</b> (destination):");
                }
                case DESTINATION -> {
                    session.setDestination(text);
                    session.setStep(Step.LINK);
                    telegramApi.sendMessage(chatId, "GitHub-ссылка (<code>https://github.com/owner/repo</code>):");
                }
                case LINK -> {
                    session.setLink(text);
                    session.setStep(Step.PARTNER_ID);
                    telegramApi.sendMessage(chatId, "UUID партнёра.\nСписок: /partner_list");
                }
                case PARTNER_ID -> {
                    session.setPartnerId(UUID.fromString(text.trim()));
                    create(chatId, session);
                    sessions.clearOrderSession(chatId);
                }
            }
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный UUID. Ещё раз или /cancel");
        } catch (Exception e) {
            log.error("order_create dialog chatId={}: {}", chatId, e.toString());
            sessions.clearOrderSession(chatId);
            telegramApi.sendMessage(chatId, "Ошибка: " + e.getMessage() + "\n/order_create");
        }
    }

    private void create(long chatId, CreateOrderSession session) {
        OrderRequest request = new OrderRequest();
        request.setName(session.getName());
        request.setSource(session.getSource());
        request.setDestination(session.getDestination());
        request.setLink(session.getLink());
        request.setPartnerId(session.getPartnerId());
        try {
            OrderResponse created = orderClient.createOrder(request);
            telegramApi.sendMessage(chatId, formatter.orderCreated(created));
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "Ошибка (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "Сервис недоступен: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
