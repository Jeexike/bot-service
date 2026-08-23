package com.example.orderproxy.telegram;

import com.example.orderproxy.client.OrderClient;
import com.example.orderproxy.client.PartnerClient;
import com.example.orderproxy.dto.OrderRequest;
import com.example.orderproxy.dto.OrderResponse;
import com.example.orderproxy.dto.PartnerRequest;
import com.example.orderproxy.dto.PartnerResponse;
import com.example.orderproxy.telegram.CreateOrderSession.Step;
import com.example.orderproxy.telegram.dto.TelegramDtos.Message;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTelegramHandler {

    private static final String HELP = """
            <b>Order Proxy Bot</b>

            /start — приветствие
            /help — эта справка

            <b>Партнёры</b>
            /partner — создать партнёра
            /partners — список партнёров
            /deletepartner &lt;uuid&gt; — удалить партнёра

            <b>Заказы</b>
            /create — создать заказ (пошагово)
            /orders — все заказы
            /orders &lt;partnerId&gt; — заказы партнёра
            /delete &lt;uuid&gt; — удалить заказ

            /cancel — отменить текущий диалог
            """;

    private final OrderClient orderClient;
    private final PartnerClient partnerClient;
    private final TelegramApiClient telegramApi;

    private final Map<Long, CreateOrderSession> orderSessions = new ConcurrentHashMap<>();
    private final Map<Long, CreatePartnerSession> partnerSessions = new ConcurrentHashMap<>();

    public void handle(Message message) {
        if (message == null || message.getChat() == null) {
            return;
        }
        long chatId = message.getChat().getId();
        String text = message.getText() == null ? "" : message.getText().trim();

        if (text.isEmpty()) {
            telegramApi.sendMessage(chatId, "Пришлите текстовую команду. /help");
            return;
        }

        String lower = text.toLowerCase();
        if (lower.startsWith("/cancel")) {
            orderSessions.remove(chatId);
            partnerSessions.remove(chatId);
            telegramApi.sendMessage(chatId, "Диалог отменён.");
            return;
        }
        if (lower.startsWith("/start")) {
            orderSessions.remove(chatId);
            partnerSessions.remove(chatId);
            telegramApi.sendMessage(chatId, "Привет! Я бот для заказов через order-proxy.\n\n" + HELP);
            return;
        }
        if (lower.startsWith("/help")) {
            telegramApi.sendMessage(chatId, HELP);
            return;
        }
        if (lower.startsWith("/deletepartner")) {
            deletePartner(chatId, text);
            return;
        }
        if (lower.startsWith("/partners")) {
            listPartners(chatId);
            return;
        }
        if (lower.startsWith("/partner")) {
            startCreatePartner(chatId);
            return;
        }
        if (lower.startsWith("/orders")) {
            listOrders(chatId, text);
            return;
        }
        if (lower.startsWith("/delete")) {
            deleteOrder(chatId, text);
            return;
        }
        if (lower.startsWith("/create")) {
            startCreateOrder(chatId);
            return;
        }

        CreatePartnerSession partnerSession = partnerSessions.get(chatId);
        if (partnerSession != null) {
            continueCreatePartner(chatId, partnerSession, text);
            return;
        }

        CreateOrderSession orderSession = orderSessions.get(chatId);
        if (orderSession != null) {
            continueCreateOrder(chatId, orderSession, text);
            return;
        }

        telegramApi.sendMessage(chatId, "Неизвестная команда. /help");
    }

    private void startCreatePartner(long chatId) {
        orderSessions.remove(chatId);
        CreatePartnerSession session = new CreatePartnerSession();
        partnerSessions.put(chatId, session);
        telegramApi.sendMessage(chatId, "Создание партнёра.\nВведите <b>name</b> (2–255):\n/cancel — отмена");
    }

    private void continueCreatePartner(long chatId, CreatePartnerSession session, String text) {
        try {
            switch (session.getStep()) {
                case NAME -> {
                    if (text.length() < 2 || text.length() > 255) {
                        telegramApi.sendMessage(chatId, "Имя: 2–255 символов. Ещё раз:");
                        return;
                    }
                    session.setName(text);
                    session.setStep(CreatePartnerSession.Step.EMAIL);
                    telegramApi.sendMessage(
                            chatId, "Введите <b>email</b> или отправьте <code>-</code>, чтобы пропустить:");
                }
                case EMAIL -> {
                    if (!"-".equals(text.trim()) && !text.isBlank()) {
                        session.setEmail(text.trim());
                    }
                    createPartner(chatId, session);
                    partnerSessions.remove(chatId);
                }
            }
        } catch (Exception e) {
            log.error("Create partner dialog error chatId={}: {}", chatId, e.toString());
            partnerSessions.remove(chatId);
            telegramApi.sendMessage(chatId, "Ошибка: " + e.getMessage() + "\nДиалог сброшен. /partner");
        }
    }

    private void createPartner(long chatId, CreatePartnerSession session) {
        PartnerRequest request = new PartnerRequest();
        request.setName(session.getName());
        request.setEmail(session.getEmail());
        try {
            PartnerResponse created = partnerClient.createPartner(request);
            telegramApi.sendMessage(chatId, formatPartner(created, "✅ Партнёр создан"));
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "❌ Ошибка создания (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void listPartners(long chatId) {
        try {
            List<PartnerResponse> partners = partnerClient.getAllPartners();
            if (partners == null || partners.isEmpty()) {
                telegramApi.sendMessage(chatId, "Партнёров нет. Создай: /partner");
                return;
            }
            StringBuilder sb = new StringBuilder("<b>Партнёры (" + partners.size() + ")</b>\n\n");
            int i = 0;
            for (PartnerResponse p : partners) {
                if (i++ >= 15) {
                    sb.append("… и ещё ").append(partners.size() - 15).append("\n");
                    break;
                }
                sb.append(formatPartnerBlock(p)).append("\n");
            }
            sb.append("Заказы партнёра: <code>/orders &lt;partnerId&gt;</code>\n");
            sb.append("Удалить: <code>/deletepartner &lt;uuid&gt;</code>");
            telegramApi.sendMessage(chatId, sb.toString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void deletePartner(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            telegramApi.sendMessage(chatId, "Использование: /deletepartner &lt;uuid&gt;");
            return;
        }
        try {
            UUID id = UUID.fromString(parts[1].trim());
            partnerClient.deletePartner(id);
            telegramApi.sendMessage(
                    chatId,
                    "✅ Партнёр <code>" + id + "</code> удалён\n" + "(связанные заказы тоже удаляются каскадно)");
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный UUID");
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "❌ Ошибка удаления (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void startCreateOrder(long chatId) {
        partnerSessions.remove(chatId);
        CreateOrderSession session = new CreateOrderSession();
        orderSessions.put(chatId, session);
        telegramApi.sendMessage(
                chatId,
                "Создание заказа.\n"
                        + "Сначала нужен <b>partnerId</b> — смотри /partners\n\n"
                        + "Введите <b>name</b> (2–255):\n/cancel — отмена");
    }

    private void continueCreateOrder(long chatId, CreateOrderSession session, String text) {
        try {
            switch (session.getStep()) {
                case NAME -> {
                    session.setName(text);
                    session.setStep(Step.SOURCE);
                    telegramApi.sendMessage(chatId, "Введите <b>source</b> (город отправки):");
                }
                case SOURCE -> {
                    session.setSource(text);
                    session.setStep(Step.DESTINATION);
                    telegramApi.sendMessage(chatId, "Введите <b>destination</b> (город доставки):");
                }
                case DESTINATION -> {
                    session.setDestination(text);
                    session.setStep(Step.LINK);
                    telegramApi.sendMessage(chatId, "Введите <b>GitHub link</b>[](https://github.com/owner/repo):");
                }
                case LINK -> {
                    session.setLink(text);
                    session.setStep(Step.PARTNER_ID);
                    telegramApi.sendMessage(chatId, "Введите <b>partnerId</b> (UUID).\nСписок: /partners");
                }
                case PARTNER_ID -> {
                    session.setPartnerId(UUID.fromString(text.trim()));
                    createOrder(chatId, session);
                    orderSessions.remove(chatId);
                }
            }
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный UUID. Ещё раз или /cancel");
        } catch (Exception e) {
            log.error("Create order dialog error chatId={}: {}", chatId, e.toString());
            orderSessions.remove(chatId);
            telegramApi.sendMessage(chatId, "Ошибка: " + e.getMessage() + "\nДиалог сброшен. /create");
        }
    }

    private void createOrder(long chatId, CreateOrderSession session) {
        OrderRequest request = new OrderRequest();
        request.setName(session.getName());
        request.setSource(session.getSource());
        request.setDestination(session.getDestination());
        request.setLink(session.getLink());
        request.setPartnerId(session.getPartnerId());
        try {
            OrderResponse created = orderClient.createOrder(request);
            telegramApi.sendMessage(chatId, formatOrder(created, "✅ Заказ создан"));
        } catch (HttpClientErrorException e) {
            telegramApi.sendMessage(
                    chatId, "❌ Ошибка создания (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис заказов недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void deleteOrder(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            telegramApi.sendMessage(chatId, "Использование: /delete &lt;uuid&gt;");
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
                    chatId, "❌ Ошибка удаления (" + e.getStatusCode().value() + "):\n" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис заказов недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void listOrders(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        try {
            List<OrderResponse> orders;
            String header;
            if (parts.length >= 2) {
                UUID partnerId = UUID.fromString(parts[1].trim());
                orders = partnerClient.getOrdersByPartnerId(partnerId);
                header = "<b>Заказы партнёра</b>\npartnerId: <code>" + partnerId + "</code>\n\n";
            } else {
                orders = orderClient.getOrders();
                header = "<b>Все заказы</b>\n\n";
            }
            if (orders == null || orders.isEmpty()) {
                telegramApi.sendMessage(chatId, header + "Заказов нет");
                return;
            }
            StringBuilder sb = new StringBuilder(header);
            sb.append("Всего: <b>").append(orders.size()).append("</b>\n\n");
            int i = 0;
            for (OrderResponse o : orders) {
                if (i++ >= 10) {
                    sb.append("… и ещё ").append(orders.size() - 10).append("\n");
                    break;
                }
                sb.append(formatOrderBlock(o)).append("\n");
            }
            telegramApi.sendMessage(chatId, sb.toString());
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный partnerId. /orders или /orders &lt;uuid&gt;");
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "❌ Сервис заказов недоступен: " + e.getMessage());
        } catch (Exception e) {
            telegramApi.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private String formatPartner(PartnerResponse p, String title) {
        return title
                + "\n"
                + formatPartnerBlock(p)
                + "\nЭтот id нужен при /create заказа.\n"
                + "Заказы: <code>/orders "
                + p.getId()
                + "</code>";
    }

    private String formatPartnerBlock(PartnerResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("👤 <b>").append(esc(p.getName())).append("</b>\n");
        sb.append("id: <code>").append(p.getId()).append("</code>\n");
        sb.append("email: ").append(nullToDash(p.getEmail())).append("\n");
        if (p.getCreatedAt() != null) {
            sb.append("created: ").append(fmt(p.getCreatedAt())).append("\n");
        }
        return sb.toString();
    }

    private String formatOrder(OrderResponse o, String title) {
        return title + "\n" + formatOrderBlock(o);
    }

    private String formatOrderBlock(OrderResponse o) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 <b>").append(esc(o.getName())).append("</b>\n");
        sb.append("id: <code>").append(o.getId()).append("</code>\n");
        sb.append("route: ")
                .append(esc(o.getSource()))
                .append(" → ")
                .append(esc(o.getDestination()))
                .append("\n");
        sb.append("partnerId: <code>")
                .append(o.getPartnerId() != null ? o.getPartnerId() : "—")
                .append("</code>\n");
        if (o.getLink() != null) {
            sb.append("link: ").append(esc(o.getLink())).append("\n");
        }
        sb.append("created: ").append(fmt(o.getCreatedAt())).append("\n");
        sb.append("updated: ").append(fmt(o.getUpdatedAt())).append("\n");
        return sb.toString();
    }

    private static String fmt(Timestamp ts) {
        return ts == null ? "—" : ts.toInstant().toString();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String esc(String s) {
        if (s == null) {
            return "—";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
