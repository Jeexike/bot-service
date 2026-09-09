package com.example.botservice.telegram.command;

import com.example.botservice.client.PartnerClient;
import com.example.botservice.dto.PartnerResponse;
import com.example.botservice.telegram.TelegramApiClient;
import com.example.botservice.telegram.TelegramMessageFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PartnerListCommand implements BotCommand {

    private final PartnerClient partnerClient;
    private final TelegramApiClient telegramApi;
    private final TelegramMessageFormatter formatter;

    @Override
    public String name() {
        return "partner_list";
    }

    @Override
    public String description() {
        return "список партнёров";
    }

    @Override
    public boolean supports(long chatId, String text) {
        return HelpCommand.startsWithCommand(text, name());
    }

    @Override
    public void handle(long chatId, String text) {
        try {
            List<PartnerResponse> partners = partnerClient.getAllPartners();
            if (partners == null || partners.isEmpty()) {
                telegramApi.sendMessage(chatId, "Партнёров нет. Создать: /partner_create");
                return;
            }
            StringBuilder sb = new StringBuilder("<b>Партнёры (" + partners.size() + ")</b>\n\n");
            int i = 0;
            for (PartnerResponse p : partners) {
                if (i++ >= 15) {
                    sb.append("… и ещё ").append(partners.size() - 15).append("\n");
                    break;
                }
                sb.append(formatter.partnerBlock(p)).append("\n");
            }
            sb.append("Заказы: <code>/order_list &lt;partnerId&gt;</code>\n");
            sb.append("Удалить: <code>/partner_delete &lt;uuid&gt;</code>");
            telegramApi.sendMessage(chatId, sb.toString());
        } catch (RestClientException e) {
            telegramApi.sendMessage(chatId, "Сервис недоступен: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
