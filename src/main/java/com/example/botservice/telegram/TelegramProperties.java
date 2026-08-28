package com.example.botservice.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramProperties {

    private String token;
    private String username;
    private int pollTimeoutSeconds;
    private boolean enabled;
}
