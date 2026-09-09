package com.example.botservice.telegram;

import com.example.botservice.dto.OrderResponse;
import com.example.botservice.dto.PartnerResponse;
import com.example.botservice.kafka.dto.OrderLinkChangedEvent;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {

    public String orderLinkChanged(OrderLinkChangedEvent event, OrderResponse order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Изменения в GitHub-репозитории\n");
        if (order != null) {
            sb.append("Заказ: <b>").append(esc(order.getName())).append("</b>\n");
        }
        sb.append("orderId: <code>").append(event.orderId()).append("</code>\n");
        sb.append("Репозиторий: ").append(esc(event.link())).append("\n");
        sb.append("Изменённые поля: ").append(changedFields(event.changedFields())).append("\n");
        sb.append("Обнаружено: ").append(fmt(event.detectedAt()));
        return sb.toString();
    }

    private static String changedFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "-";
        }
        return String.join(", ", fields);
    }

    private static String fmt(OffsetDateTime ts) {
        return ts == null ? "-" : ts.toString();
    }

    public String partnerCreated(PartnerResponse p) {
        return "Партнёр создан\n"
                + partnerBlock(p)
                + "\nId нужен при создании заказа: /order_create\n"
                + "Заказы партнёра: <code>/order_list "
                + p.getId()
                + "</code>";
    }

    public String partnerBlock(PartnerResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(esc(p.getName())).append("</b>\n");
        sb.append("id: <code>").append(p.getId()).append("</code>\n");
        sb.append("email: ").append(dash(p.getEmail())).append("\n");
        if (p.getCreatedAt() != null) {
            sb.append("created: ").append(fmt(p.getCreatedAt())).append("\n");
        }
        return sb.toString();
    }

    public String orderCreated(OrderResponse o) {
        return "Заказ создан\n" + orderBlock(o);
    }

    public String orderBlock(OrderResponse o) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(esc(o.getName())).append("</b>\n");
        sb.append("id: <code>").append(o.getId()).append("</code>\n");
        sb.append("маршрут: ")
                .append(esc(o.getSource()))
                .append(" → ")
                .append(esc(o.getDestination()))
                .append("\n");
        sb.append("partnerId: <code>")
                .append(o.getPartnerId() != null ? o.getPartnerId() : "-")
                .append("</code>\n");
        if (o.getLink() != null) {
            sb.append("link: ").append(esc(o.getLink())).append("\n");
        }
        sb.append("created: ").append(fmt(o.getCreatedAt())).append("\n");
        sb.append("updated: ").append(fmt(o.getUpdatedAt())).append("\n");
        return sb.toString();
    }

    private static String fmt(Timestamp ts) {
        return ts == null ? "-" : ts.toInstant().toString();
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String esc(String s) {
        if (s == null) return "-";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}