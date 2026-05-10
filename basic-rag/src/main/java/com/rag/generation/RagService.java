package com.rag.generation;

import com.rag.model.DocumentChunk;
import com.rag.model.RagResponse;
import com.rag.retrieval.InMemoryVectorStore;
import com.rag.retrieval.PgVectorStore;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final EmbeddingModel embeddingModel;
//    private final InMemoryVectorStore vectorStore;
    private final PgVectorStore vectorStore;
    private final ChatModel chatModel;

    @Value("${rag.score-threshold:0.7}")
    private double scoreThreshold;

    public RagService(EmbeddingModel embeddingModel,
                      PgVectorStore vectorStore,
                      ChatModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

//    public String ask(String question) {
//        float[] questionEmbedding = embeddingModel.embed(question);
//
//        List<DocumentChunk> relevantChunks = vectorStore.similaritySearch(questionEmbedding);
//
//        if (relevantChunks.isEmpty()) {
//            return "No documents have been ingested yet. Please upload a document first.";
//        }
//
//        String context = relevantChunks.stream()
//                .map(DocumentChunk::content)
//                .collect(Collectors.joining("\n\n---\n\n"));
//
//        String promptText = """
//                You are a helpful assistant. Answer the user's question using ONLY \
//                the context provided below. If the answer is not in the context, \
//                say "I don't have enough information to answer that."
//
//                Context:
//                %s
//
//                Question: %s
//
//                Answer:
//                """.formatted(context, question);
//
//        return chatModel.call(new Prompt(promptText))
//                .getResult()
//                .getOutput()
//                .getText();
//    }

    public RagResponse ask(String question) {
        float[] questionEmbedding = embeddingModel.embed(question);

        // Retrieve and filter by score threshold
        List<DocumentChunk> relevantChunks = vectorStore
                .similaritySearch(questionEmbedding)
                .stream()
                .filter(chunk -> chunk.score() >= scoreThreshold)
                .toList();

        if (relevantChunks.isEmpty()) {
            return new RagResponse(
                    "I don't have enough relevant information to answer that question.",
                    List.of()
            );
        }

        String context = relevantChunks.stream()
                .map(DocumentChunk::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        String promptText = """
                You are a helpful assistant. Answer the user's question using ONLY \
                the context provided below. If the answer is not in the context, \
                say "I don't have enough information to answer that."
                
                Context:
                %s
                
                Question: %s
                
                Answer:
                """.formatted(context, question);

        String answer = chatModel.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();

        // Build source citations
        List<RagResponse.Source> sources = relevantChunks.stream()
                .map(chunk -> new RagResponse.Source(
                        chunk.source(),
                        chunk.content().substring(0, Math.min(100, chunk.content().length())) + "...",
                        chunk.score()
                ))
                .toList();

        return new RagResponse(answer, sources);
    }
}
