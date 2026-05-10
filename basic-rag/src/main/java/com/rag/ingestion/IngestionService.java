package com.rag.ingestion;

import com.rag.model.DocumentChunk;
import com.rag.retrieval.InMemoryVectorStore;
import com.rag.retrieval.PgVectorStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {

    private static final int BATCH_SIZE = 10;

    private final TextChunker chunker;
    private final EmbeddingModel embeddingModel;
//    private final InMemoryVectorStore vectorStore;
    private final PgVectorStore vectorStore;

    public IngestionService(TextChunker chunker,
                            EmbeddingModel embeddingModel,
                            PgVectorStore vectorStore) {
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    public int ingest(MultipartFile file) throws IOException {
        String text = extractText(file);
        List<String> chunks = chunker.chunk(text);

        int totalIngested = 0;

        // Process in batches — keeps memory flat regardless of document size
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<String> batch = chunks.subList(i, end);

            List<DocumentChunk> batchChunks = new ArrayList<>();
            for (String chunkContent : batch) {
                float[] embedding = embeddingModel.embed(chunkContent);
                batchChunks.add(new DocumentChunk(
                        UUID.randomUUID().toString(),
                        chunkContent,
                        file.getOriginalFilename(),
                        embedding,
                        0.0
                ));
            }

            vectorStore.addAll(batchChunks);
            totalIngested += batchChunks.size();

            // Let GC breathe between batches
            batchChunks.clear();
            System.out.printf("Ingested batch %d/%d%n",
                    Math.min(i + BATCH_SIZE, chunks.size()), chunks.size());
        }

        return totalIngested;
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(doc);
            }
        }

        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}