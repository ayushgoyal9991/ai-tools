package com.rag.model;

import java.util.List;

public record AskResponse(
        String answer,
        List<RagResponse.Source> sources,
        String originalQuestion,
        String rewrittenQuery
) {}
