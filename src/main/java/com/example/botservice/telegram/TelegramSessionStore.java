package com.example.botservice.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TelegramSessionStore {

    private final Map<Long, CreatePartnerSession> partnerSessions = new ConcurrentHashMap<>();
    private final Map<Long, CreateOrderSession> orderSessions = new ConcurrentHashMap<>();

    public CreatePartnerSession getPartnerSession(long chatId) {
        return partnerSessions.get(chatId);
    }

    public void putPartnerSession(long chatId, CreatePartnerSession session) {
        orderSessions.remove(chatId);
        partnerSessions.put(chatId, session);
    }

    public void clearPartnerSession(long chatId) {
        partnerSessions.remove(chatId);
    }

    public CreateOrderSession getOrderSession(long chatId) {
        return orderSessions.get(chatId);
    }

    public void putOrderSession(long chatId, CreateOrderSession session) {
        partnerSessions.remove(chatId);
        orderSessions.put(chatId, session);
    }

    public void clearOrderSession(long chatId) {
        orderSessions.remove(chatId);
    }

    public void clearAll(long chatId) {
        partnerSessions.remove(chatId);
        orderSessions.remove(chatId);
    }

    public boolean hasActiveDialog(long chatId) {
        return partnerSessions.containsKey(chatId) || orderSessions.containsKey(chatId);
    }
}
