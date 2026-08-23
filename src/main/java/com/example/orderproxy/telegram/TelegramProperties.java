package com.example.orderproxy.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramProperties {

    private String token = "";

    private String username = "";

    private int pollTimeoutSeconds = 30;

    private boolean enabled = true;
}
