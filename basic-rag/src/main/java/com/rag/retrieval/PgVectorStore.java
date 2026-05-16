package com.rag.retrieval;

import com.rag.model.DocumentChunk;
import com.rag.model.DocumentChunkEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PgVectorStore {

    private final DocumentChunkRepository repository;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.keyword.threshold:0.1}")
    private double keywordThreshold;

    @Value("${rag.vector.weight:0.7}")
    private double vectorWeight;

    @Value("${rag.keyword.weight:0.3}")
    private double keywordWeight;

    public PgVectorStore(DocumentChunkRepository repository) {
        this.repository = repository;
    }

    public void add(DocumentChunk chunk) {
        repository.insertWithVector(
                chunk.id(),
                chunk.content(),
                chunk.source(),
                toVectorString(chunk.embedding())
        );
    }

    public void addAll(List<DocumentChunk> chunks) {
        chunks.forEach(this::add);
    }

//    public List<DocumentChunk> similaritySearch(float[] queryEmbedding) {
//        return repository.findTopKBySimilarity(toVectorString(queryEmbedding), topK)
//                .stream()
//                .map(this::toDomain)
//                .toList();
//    }
public List<DocumentChunk> similaritySearch(float[] queryEmbedding) {
    String vectorString = toVectorString(queryEmbedding);
    return repository.findTopKBySimilarity(vectorString, topK)
            .stream()
            .map(this::toDomainWithScore)
            .toList();
}

    // Hybrid search — vector + keyword combined
    public List<DocumentChunk> hybridSearch(float[] queryEmbedding, String queryText) {
        // Run both searches
        List<DocumentChunk> vectorResults = repository
                .findTopKBySimilarity(toVectorString(queryEmbedding), topK)
                .stream()
                .map(this::toDomainWithScore)
                .toList();

        List<DocumentChunk> keywordResults = repository
                .findTopKByKeyword(queryText, keywordThreshold, topK)
                .stream()
                .map(this::toDomainWithScore)
                .toList();

        // Merge and re-rank using weighted scores
        // Use LinkedHashMap to deduplicate by chunk id
        Map<String, DocumentChunk> merged = new LinkedHashMap<>();

        for (DocumentChunk chunk : vectorResults) {
            merged.put(chunk.id(), new DocumentChunk(
                    chunk.id(),
                    chunk.content(),
                    chunk.source(),
                    chunk.embedding(),
                    chunk.score() * vectorWeight
            ));
        }

        for (DocumentChunk chunk : keywordResults) {
            if (merged.containsKey(chunk.id())) {
                // Chunk found by both — combine scores
                merged.computeIfPresent(chunk.id(), (k, existing) -> new DocumentChunk(
                        chunk.id(),
                        chunk.content(),
                        chunk.source(),
                        chunk.embedding(),
                        existing.score() + (chunk.score() * keywordWeight)
                ));
            } else {
                // Keyword-only result
                merged.put(chunk.id(), new DocumentChunk(
                        chunk.id(),
                        chunk.content(),
                        chunk.source(),
                        chunk.embedding(),
                        chunk.score() * keywordWeight
                ));
            }
        }

        // Sort by combined score descending
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(DocumentChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    public void clear() {
        repository.deleteAll();
    }

    public long size() {
        return repository.count();
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private DocumentChunk toDomainWithScore(Object[] row) {
        String id       = (String) row[0];
        String content  = (String) row[1];
        String source   = (String) row[2];
        String embText  = (String) row[3];
        double score    = ((Number) row[4]).doubleValue();

        float[] embedding = parseEmbedding(embText);
        return new DocumentChunk(id, content, source, embedding, score);
    }

//    private DocumentChunk toDomain(DocumentChunkEntity entity) {
//        // Parse the text embedding back to float[]
//        String raw = entity.getEmbedding()
//                .replace("[", "")
//                .replace("]", "");
//        String[] parts = raw.split(",");
//        float[] embedding = new float[parts.length];
//        for (int i = 0; i < parts.length; i++) {
//            embedding[i] = Float.parseFloat(parts[i].trim());
//        }
//        return new DocumentChunk(
//                entity.getId(),
//                entity.getContent(),
//                entity.getSource(),
//                embedding
//        );
//    }

    private DocumentChunk toDomain(DocumentChunkEntity entity) {
        return new DocumentChunk(
                entity.getId(),
                entity.getContent(),
                entity.getSource(),
                entity.getEmbedding(),  // float[] directly, no parsing needed
                0.0
        );
    }

    private float[] parseEmbedding(String raw) {
        String cleaned = raw.replace("[", "").replace("]", "");
        String[] parts = cleaned.split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Float.parseFloat(parts[i].trim());
        }
        return embedding;
    }
}