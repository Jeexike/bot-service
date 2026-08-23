package com.example.orderproxy.telegram;

import lombok.Data;

@Data
public class CreatePartnerSession {

    public enum Step {
        NAME,
        EMAIL
    }

    private Step step = Step.NAME;
    private String name;
    private String email;
}
