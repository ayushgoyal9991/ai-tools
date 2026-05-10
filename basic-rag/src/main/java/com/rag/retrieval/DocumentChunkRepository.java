package com.rag.retrieval;

import com.rag.model.DocumentChunkEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, String> {

    // Similarity search — cast text to vector at query time
//    @Query(value = """
//            SELECT id, content, source, embedding::text
//            FROM document_chunks
//            ORDER BY embedding::vector <=> CAST(:embedding AS vector)
//            LIMIT :topK
//            """, nativeQuery = true)
//    List<DocumentChunkEntity> findTopKBySimilarity(
//            @Param("embedding") String embedding,
//            @Param("topK") int topK
//    );

//    @Query(value = """
//        SELECT id, content, source, embedding::text,
//               1 - (embedding::vector <=> CAST(:embedding AS vector)) AS score
//        FROM document_chunks
//        ORDER BY embedding::vector <=> CAST(:embedding AS vector)
//        LIMIT :topK
//        """, nativeQuery = true)
//    List<Object[]> findTopKBySimilarity(
//            @Param("embedding") String embedding,
//            @Param("topK") int topK
//    );
//
//    // Insert with explicit vector cast
//    @Modifying
//    @Transactional
//    @Query(value = """
//            INSERT INTO document_chunks (id, content, source, embedding)
//            VALUES (:id, :content, :source, CAST(:embedding AS vector))
//            """, nativeQuery = true)
//    void insertWithVector(
//            @Param("id") String id,
//            @Param("content") String content,
//            @Param("source") String source,
//            @Param("embedding") String embedding
//    );

    @Query(value = """
            SELECT id, content, source, embedding::text,
                   1 - (embedding <=> CAST(:embedding AS vector)) AS score
            FROM document_chunks
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findTopKBySimilarity(
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO document_chunks (id, content, source, embedding)
            VALUES (:id, :content, :source, CAST(:embedding AS vector))
            """, nativeQuery = true)
    void insertWithVector(
            @Param("id") String id,
            @Param("content") String content,
            @Param("source") String source,
            @Param("embedding") String embedding
    );
}
