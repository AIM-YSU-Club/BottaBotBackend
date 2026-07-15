package club.ysu_aim.botta.Document;

import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 벡터화를 위해 쪼개진 문서 청크.
 * Source 1건 → Document N건 (기존 source_map M:N은 ERD상 제거, source_id FK로 변경).
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

    /** 소속 노트북 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    /** 원본 소스 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    /** 본문(청크) */
    @Column(columnDefinition = "TEXT")
    private String chunk;
    
    // PostgreSQL pgvector 연동을 고려해 데이터 타입을 vector로 명시하고 float[] 매핑
    @Column(columnDefinition = "vector")
    private float[] embeddings;
}
