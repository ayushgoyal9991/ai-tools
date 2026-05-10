package com.rag.model;

public record DocumentChunk(
        String id,
        String content,
        String source,
        float[] embedding,
        double score
) {}
