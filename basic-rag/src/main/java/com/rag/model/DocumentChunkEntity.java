package com.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_chunks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String source;

//    @Column(columnDefinition = "TEXT")
//    private String embedding;

    @Column(columnDefinition = "vector(768)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] embedding;
}
