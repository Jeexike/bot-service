package com.example.orderproxy.util;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

public final class SafeResultActions {

    private final ResultActions delegate;

    public SafeResultActions(ResultActions delegate) {
        this.delegate = delegate;
    }

    public SafeResultActions andExpect(ResultMatcher matcher) {
        try {
            delegate.andExpect(matcher);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
