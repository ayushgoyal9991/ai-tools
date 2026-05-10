package com.rag.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("recursive")
public class RecursiveChunkingStrategy implements ChunkingStrategy {

    @Value("${rag.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    private static final String[] SEPARATORS = {
            "\n\n",   // paragraph boundary — most preferred
            "\n",     // line boundary
            ". ",     // sentence boundary
            " ",      // word boundary
            ""        // character — last resort
    };

    @Override
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        splitRecursively(text, 0, chunks);
        return chunks;
    }

    private void splitRecursively(String text, int separatorIndex, List<String> result) {
        if (text.length() <= chunkSize) {
            if (!text.isBlank()) result.add(text.trim());
            return;
        }

        if (separatorIndex >= SEPARATORS.length - 1) {
            characterSplit(text, result);
            return;
        }

        String separator = SEPARATORS[separatorIndex];
        String[] parts = separator.isEmpty()
                ? text.split("")
                : text.split(separator, -1);

        if (parts.length <= 1) {
            splitRecursively(text, separatorIndex + 1, result);
            return;
        }

        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            String candidate = current.isEmpty()
                    ? part
                    : current + separator + part;

            if (candidate.length() <= chunkSize) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.toString().isBlank()) {
                    result.add(current.toString().trim());
                }
                if (part.length() > chunkSize) {
                    splitRecursively(part, separatorIndex + 1, result);
                    current = new StringBuilder();
                } else {
                    current = new StringBuilder(part);
                }
            }
        }

        if (!current.toString().isBlank()) {
            result.add(current.toString().trim());
        }
    }

    private void characterSplit(String text, List<String> result) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end).trim());
            int next = end - chunkOverlap;
            start = next <= start ? end : next;
        }
    }
}
