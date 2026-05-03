package com.rag.api;

import com.rag.generation.RagService;
import com.rag.ingestion.IngestionService;
import com.rag.model.AskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final IngestionService ingestionService;
    private final RagService ragService;

    public RagController(IngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @Operation(summary = "Ingest a document", description = "Upload a PDF or TXT file")
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingest(
            @Parameter(description = "PDF or TXT file to ingest")
            @RequestParam("file") MultipartFile file) {
        try {
            int chunks = ingestionService.ingest(file);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "file", file.getOriginalFilename(),
                    "chunks", chunks
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @Operation(summary = "Ask a question", description = "Query the ingested documents using RAG")
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody AskRequest body) {
        String question = body.question();
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "question field is required"));
        }
        String answer = ragService.ask(question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
