package com.example.botservice.telegram.command;

import com.example.botservice.client.OrderClient;
import com.example.botservice.client.PartnerClient;
import com.example.botservice.dto.OrderResponse;
import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramMessageFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class OrderListCommand implements BotCommand {

    private final OrderClient orderClient;
    private final PartnerClient partnerClient;
    private final TelegramApiClient telegramApi;
    private final TelegramMessageFormatter formatter;

    @Override
    public String name() {
        return "order_list";
    }

    @Override
    public String description() {
        return "список заказов; /order_list &lt;partnerId&gt; - по партнёру";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name());
    }

    @Override
    public void handle(long chatId, String text) {
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
                sb.append(formatter.orderBlock(o)).append("\n");
            }
            telegramApi.sendMessage(chatId, sb.toString());
        } catch (IllegalArgumentException e) {
            telegramApi.sendMessage(chatId, "Некорректный partnerId");
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "Сервис недоступен: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
