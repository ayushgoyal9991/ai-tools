package com.rag.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("fixedSize")
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    @Value("${rag.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Override
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            if (end < length) {
                // 1. Try sentence boundary
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start + (chunkSize / 2)) {
                    end = lastPeriod + 1;
                }
                // 2. Try word boundary
                else {
                    int lastSpace = text.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace + 1;
                    }
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            int next = end - chunkOverlap;
            start = next <= start ? end : next;
        }

        return chunks;
    }
}

