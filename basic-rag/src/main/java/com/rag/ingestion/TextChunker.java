package com.rag.ingestion;

import com.rag.chunking.ChunkingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TextChunker {

//    @Value("${rag.chunk-size:500}")
//    private int chunkSize;
//
//    @Value("${rag.chunk-overlap:50}")
//    private int chunkOverlap;

//    public List<String> chunk(String text) {
//        List<String> chunks = new ArrayList<>();
//        int length = text.length();
//        int start = 0;
//
//        while (start < length) {
//            int end = Math.min(start + chunkSize, length);
//
//            // Try to break at sentence boundary
//            if (end < length) {
//                int lastPeriod = text.lastIndexOf('.', end);
//                if (lastPeriod > start + (chunkSize / 2)) {
//                    end = lastPeriod + 1;
//                }
//            }
//
//            // Extract chunk and skip blank ones — no full-string copies
//            String chunk = text.substring(start, end);
//            if (!chunk.isBlank()) {
//                chunks.add(chunk);
//            }
//
//            int next = end - chunkOverlap;
//            if (next <= start) {
//                start = end; // safety guard against infinite loop
//            } else {
//                start = next;
//            }
//        }
//
//        return chunks;
//    }

    private final Map<String, ChunkingStrategy> strategies;

    @Value("${rag.chunking-strategy:fixedSize}")
    private String strategyName;

    public TextChunker(Map<String, ChunkingStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<String> chunk(String text) {
        ChunkingStrategy strategy = strategies.getOrDefault(
                strategyName,
                strategies.get("fixedSize")
        );
        System.out.println("Using chunking strategy: " + strategyName);
        return strategy.chunk(text);
    }
}
