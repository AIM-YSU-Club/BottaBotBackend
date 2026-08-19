package club.ysu_aim.botta.Document;

import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 벡터화를 위해 쪼개진 문서 청크.
 * 원본 소스에 소속되며, 노트북 범위는 source → notebook 경로로 식별한다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Document {

    /** 문서 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    /** 소속 소스 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    /** 본문(청크) */
    @Column(columnDefinition = "TEXT")
    private String chunk;

    /** 임베딩된 벡터 (pgvector) */
    @Column(columnDefinition = "vector")
    private float[] embeddings;
}
