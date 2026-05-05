package com.rag.generation;

import com.rag.model.DocumentChunk;
import com.rag.retrieval.InMemoryVectorStore;
import com.rag.retrieval.PgVectorStore;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final EmbeddingModel embeddingModel;
//    private final InMemoryVectorStore vectorStore;
    private final PgVectorStore vectorStore;
    private final ChatModel chatModel;

    public RagService(EmbeddingModel embeddingModel,
                      PgVectorStore vectorStore,
                      ChatModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    public String ask(String question) {
        float[] questionEmbedding = embeddingModel.embed(question);

        List<DocumentChunk> relevantChunks = vectorStore.similaritySearch(questionEmbedding);

        if (relevantChunks.isEmpty()) {
            return "No documents have been ingested yet. Please upload a document first.";
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

        return chatModel.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();
    }
}
