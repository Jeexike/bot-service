package com.example.botservice.telegram;

import java.util.UUID;
import lombok.Data;

@Data
public class CreateOrderSession {

    public enum Step {
        NAME,
        SOURCE,
        DESTINATION,
        LINK,
        PARTNER_ID
    }

    private Step step = Step.NAME;
    private String name;
    private String source;
    private String destination;
    private String link;
    private UUID partnerId;
}
