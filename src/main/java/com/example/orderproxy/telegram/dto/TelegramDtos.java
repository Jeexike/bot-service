package com.example.orderproxy.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

public final class TelegramDtos {

    private TelegramDtos() {}

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetUpdatesResponse {
        private boolean ok;
        private List<Update> result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Update {
        @JsonProperty("update_id")
        private long updateId;

        private Message message;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty("message_id")
        private long messageId;

        private Chat chat;
        private String text;
        private User from;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chat {
        private long id;
        private String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private long id;
        private String username;

        @JsonProperty("first_name")
        private String firstName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendMessageResponse {
        private boolean ok;
    }
}