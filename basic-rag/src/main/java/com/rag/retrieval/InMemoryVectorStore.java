package com.rag.retrieval;

import com.rag.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryVectorStore {
    private final CopyOnWriteArrayList<DocumentChunk> store = new CopyOnWriteArrayList<>();

    @Value("${rag.top-k:3}")
    private int topK;

    public void add(DocumentChunk chunk) {
        store.add(chunk);
    }

    public void addAll(List<DocumentChunk> chunks) {
        store.addAll(chunks);
    }

    public List<DocumentChunk> similaritySearch(float[] queryEmbedding) {
        return store.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, chunk.embedding())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .toList();
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }

    private record ScoredChunk(DocumentChunk chunk, double score) {}

}
