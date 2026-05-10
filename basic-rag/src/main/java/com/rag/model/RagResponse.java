package com.rag.model;

import java.util.List;

public record RagResponse(
        String answer,
        List<Source> sources
) {
    public record Source(
            String file,
            String excerpt,
            double score
    ) {}
}
