package com.rag.history;

import com.rag.model.ConversationMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@SessionScope
public class ConversationHistory {

    private final List<ConversationMessage> messages = new ArrayList<>();

    private static final int MAX_HISTORY = 10; // keep last 10 exchanges

    public void addUserMessage(String content) {
        messages.add(new ConversationMessage("user", content));
        trim();
    }

    public void addAssistantMessage(String content) {
        messages.add(new ConversationMessage("assistant", content));
        trim();
    }

    public List<ConversationMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void clear() {
        messages.clear();
    }

    // Keep only last MAX_HISTORY messages to avoid prompt bloat
    private void trim() {
        if (messages.size() > MAX_HISTORY * 2) {
            messages.subList(0, 2).clear();
        }
    }
}
