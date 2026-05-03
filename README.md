# Basic RAG — Java + Spring Boot + Ollama

A beginner-friendly **Retrieval Augmented Generation (RAG)** application built with Java, Spring Boot, and Ollama. Upload any PDF or TXT document and ask questions about it — all running locally with no API costs.

---

## What is RAG?

Instead of relying on what an LLM memorized during training, RAG **fetches relevant context from your documents at runtime** and injects it into the prompt — giving you grounded, accurate answers from your own data.

```
Your Document → Chunk → Embed → Vector Store
User Question → Embed → Similarity Search → Build Prompt → LLM → Answer
```

---

## Tech Stack

| Layer | Tool |
|---|---|
| Framework | Spring Boot 3.3.4 |
| LLM | Ollama (llama3.2) |
| Embeddings | Ollama (nomic-embed-text) |
| Vector Store | In-memory (cosine similarity) |
| PDF Parsing | Apache PDFBox 3.x |
| API Docs | SpringDoc OpenAPI (Swagger) |

---

## Prerequisites

- Java 21+
- Maven 3.8+
- [Ollama](https://ollama.com) installed on your machine

---

## Setup

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd basic-rag
```

### 2. Pull the required Ollama models

```bash
# Embedding model (required for RAG)
ollama pull nomic-embed-text

# Chat model
ollama pull llama3.2
```

Verify both are available:

```bash
ollama list
```

You should see both `llama3.2` and `nomic-embed-text` listed.

### 3. Configure the application

All settings are in `src/main/resources/application.properties`:

```properties
# Ollama connection
spring.ai.ollama.base-url=http://localhost:11434

# Models
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.3
spring.ai.ollama.embedding.model=nomic-embed-text

# Server
server.port=8080

# RAG tuning
rag.chunk-size=500
rag.chunk-overlap=50
rag.top-k=3
```

| Property | Description |
|---|---|
| `rag.chunk-size` | Characters per chunk when splitting documents |
| `rag.chunk-overlap` | Overlap between chunks to preserve context at boundaries |
| `rag.top-k` | Number of chunks to retrieve per query |
| `temperature` | Lower = more factual answers (0.0–1.0) |

### 4. Build the project

```bash
mvn clean install
```

### 5. Run the application

Make sure Ollama is running, then start the app:

```bash
# Set heap size to avoid OOM on large files
export MAVEN_OPTS="-Xms512m -Xmx2g"
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

---

## Usage

### Swagger UI

Open your browser and go to:

```
http://localhost:8080/swagger-ui.html
```

You'll see two endpoints ready to use with a file picker for uploads.

### REST API

#### Ingest a document

```bash
curl -X POST http://localhost:8080/api/rag/ingest \
  -F "file=@/path/to/your/document.pdf"
```

Response:
```json
{
  "status": "success",
  "file": "document.pdf",
  "chunks": 42
}
```

Supports `.pdf` and `.txt` files.

#### Ask a question

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this document about?"}'
```

Response:
```json
{
  "answer": "This document is about..."
}
```

---

## Project Structure

```
src/main/java/com/rag/
├── RagApplication.java          # Entry point
├── api/
│   ├── RagController.java       # REST endpoints
│   ├── AskRequest.java          # Request record
│   └── AskResponse.java         # Response record
├── ingestion/
│   ├── IngestionService.java    # Load → chunk → embed → store
│   └── TextChunker.java         # Split text into chunks
├── retrieval/
│   └── InMemoryVectorStore.java # Cosine similarity search
├── generation/
│   └── RagService.java          # Retrieve → prompt → generate
└── model/
    └── DocumentChunk.java       # Core data model
```

---

## How It Works

### Ingestion (run once per document)

1. Upload a PDF or TXT file via the `/ingest` endpoint
2. Text is extracted and split into overlapping chunks
3. Each chunk is embedded via `nomic-embed-text` into a float vector
4. Vectors are stored in memory with their source text

### Query (per request)

1. The user's question is embedded using the same model
2. Cosine similarity search finds the top-K most relevant chunks
3. Retrieved chunks are injected into a prompt as context
4. `llama3.2` generates a grounded answer using only that context

---

## Troubleshooting

**`OutOfMemoryError` during ingestion**

Increase the JVM heap before running:
```bash
export MAVEN_OPTS="-Xms512m -Xmx2g"
```

**`address already in use` on port 11434**

Ollama is already running in the background — this is fine, no action needed.

**`Could not find artifact` for Spring AI**

The project uses Spring AI `1.0.0-M6` from the Spring milestone repository. Make sure the `<repositories>` block is present in `pom.xml` and run:
```bash
rm -rf ~/.m2/repository/org/springframework/ai
mvn clean install -U
```

**No answer returned after ingestion**

Make sure Ollama is running and both models are pulled:
```bash
ollama list
curl http://localhost:11434
```

---

## What to Try Next

Once the basic pipeline is working, here are good next steps to explore:

- **Score threshold** — reject chunks with similarity below 0.7
- **Source citation** — return which file/chunk the answer came from
- **Conversation history** — pass previous Q&As into the prompt
- **Persistent vector store** — swap in-memory store for ChromaDB
- **Semantic chunking** — split by topic instead of character count
