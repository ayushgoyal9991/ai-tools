package com.rag.model;

public record AskRequest(String question, boolean clearHistory) {
}
