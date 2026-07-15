package club.ysu_aim.botta.Document;

import club.ysu_aim.botta.Notebook.Notebook;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 벡터화를 위해 쪼개진 문서 청크.
 * Notebook에만 소속되며, Source와의 직접 참조는 두지 않는다.
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

    /** 본문(청크) */
    @Column(columnDefinition = "TEXT")
    private String chunk;

    /** 임베딩된 벡터 (pgvector) */
    @Column(columnDefinition = "vector")
    private float[] embeddings;
}
