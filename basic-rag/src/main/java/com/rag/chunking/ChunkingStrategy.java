package com.rag.chunking;

import java.util.List;

public interface ChunkingStrategy {
    List<String> chunk(String text);
}
