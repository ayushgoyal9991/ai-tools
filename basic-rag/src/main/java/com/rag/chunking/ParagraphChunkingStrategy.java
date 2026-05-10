package com.rag.chunking;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("paragraph")
public class ParagraphChunkingStrategy implements ChunkingStrategy {

    @Override
    public List<String> chunk(String text) {
        return Arrays.stream(text.split("\\n\\n+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}

