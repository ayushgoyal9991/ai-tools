package com.rag.generation;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryRewriter {

    private final ChatModel chatModel;

    public QueryRewriter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String rewrite(String originalQuestion) {

        SystemMessage system = new SystemMessage("""
                You are a search query optimizer for a vector database.
                Your only job is to convert a user question into 5-8 keywords \
                that are likely to appear verbatim in a resume or document.
                Output ONLY the keywords separated by spaces on a single line.
                No explanation. No punctuation. No line breaks. No numbering.
                """);

        UserMessage user = new UserMessage("""
                Convert this question to keywords: %s
                """.formatted(originalQuestion));

        return chatModel.call(new Prompt(List.of(system, user)))
                .getResult()
                .getOutput()
                .getText()
                .trim()
                .replaceAll("\\n.*", "")   // take only first line
                .replaceAll("[\"'.,]", ""); // strip stray punctuation
    }
}
