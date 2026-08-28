package com.example.botservice.telegram;

import com.example.botservice.dto.OrderResponse;
import com.example.botservice.dto.PartnerResponse;
import java.sql.Timestamp;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {

    public String partnerCreated(PartnerResponse p) {
        return "✅ Партнёр создан\n"
                + partnerBlock(p)
                + "\nId нужен при создании заказа: /order_create\n"
                + "Заказы партнёра: <code>/order_list "
                + p.getId()
                + "</code>";
    }

    public String partnerBlock(PartnerResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("👤 <b>").append(esc(p.getName())).append("</b>\n");
        sb.append("id: <code>").append(p.getId()).append("</code>\n");
        sb.append("email: ").append(dash(p.getEmail())).append("\n");
        if (p.getCreatedAt() != null) {
            sb.append("created: ").append(fmt(p.getCreatedAt())).append("\n");
        }
        return sb.toString();
    }

    public String orderCreated(OrderResponse o) {
        return "✅ Заказ создан\n" + orderBlock(o);
    }

    public String orderBlock(OrderResponse o) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 <b>").append(esc(o.getName())).append("</b>\n");
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
