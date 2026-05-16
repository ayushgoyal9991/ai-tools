package com.rag.generation;

import com.rag.history.ConversationHistory;
import com.rag.model.ConversationMessage;
import com.rag.model.DocumentChunk;
import com.rag.model.RagResponse;
import com.rag.retrieval.InMemoryVectorStore;
import com.rag.retrieval.PgVectorStore;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final EmbeddingModel embeddingModel;
//    private final InMemoryVectorStore vectorStore;
    private final PgVectorStore vectorStore;
    private final ChatModel chatModel;
    private final QueryRewriter queryRewriter;
    private final ConversationHistory conversationHistory;

    @Value("${rag.score-threshold:0.3}")
    private double scoreThreshold;

    @Value("${rag.query-rewriting.enabled:true}")
    private boolean queryRewritingEnabled;

    public RagService(EmbeddingModel embeddingModel,
                      PgVectorStore vectorStore,
                      ChatModel chatModel, QueryRewriter queryRewriter, ConversationHistory conversationHistory) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.queryRewriter = queryRewriter;
        this.conversationHistory = conversationHistory;
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

    public RagResponse ask(String question, boolean clearHistory) {
        if (clearHistory) {
            conversationHistory.clear();
        }

        // Step 1 — Rewrite query for retrieval
        String searchQuery = question;
        String rewrittenQuery = null;

        if (queryRewritingEnabled) {
            rewrittenQuery = queryRewriter.rewrite(question);
            searchQuery = rewrittenQuery;
            System.out.println("Original  : " + question);
            System.out.println("Rewritten : " + rewrittenQuery);
        }

        // Step 2 — Embed and search
        float[] questionEmbedding = embeddingModel.embed(searchQuery);

        List<DocumentChunk> relevantChunks = vectorStore
                .hybridSearch(questionEmbedding, searchQuery)
                .stream()
                .peek(chunk -> System.out.printf(
                        "Score: %.4f | %s%n",
                        chunk.score(),
                        chunk.content().substring(0, Math.min(60, chunk.content().length()))
                ))
                .filter(chunk -> chunk.score() >= scoreThreshold)
                .toList();

        // Step 3 — Build messages list with history + context
        List<Message> messages = new ArrayList<>();

        // System message with document context
        String context = relevantChunks.isEmpty()
                ? "No relevant context found."
                : relevantChunks.stream()
                  .map(DocumentChunk::content)
                  .collect(Collectors.joining("\n\n---\n\n"));

        messages.add(new SystemMessage("""
                You are a helpful assistant. Answer the user's question using ONLY \
                the context provided below. If the answer is not in the context, \
                say "I don't have enough information to answer that."
                Use the conversation history to understand follow-up questions.
                
                Context:
                %s
                """.formatted(context)));

        // Add conversation history
        for (ConversationMessage msg : conversationHistory.getMessages()) {
            if (msg.role().equals("user")) {
                messages.add(new UserMessage(msg.content()));
            } else {
                messages.add(new AssistantMessage(msg.content()));
            }
        }

        // Add current question
        messages.add(new UserMessage(question));

        // Step 4 — Call LLM
        String answer = chatModel.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getText();

        // Step 5 — Save to history
        conversationHistory.addUserMessage(question);
        conversationHistory.addAssistantMessage(answer);

        // Step 6 — Build response
        List<RagResponse.Source> sources = relevantChunks.stream()
                .map(chunk -> new RagResponse.Source(
                        chunk.source(),
                        chunk.content().substring(0, Math.min(100, chunk.content().length())) + "...",
                        chunk.score()
                ))
                .toList();

        return new RagResponse(answer, sources, question, rewrittenQuery);
    }
}
