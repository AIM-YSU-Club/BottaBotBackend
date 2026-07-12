package club.ysu_aim.botta.Source;

import club.ysu_aim.botta.Notebook.Notebook;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 소스 — RAG에 올린 원본(파일·웹페이지·이미지 등)의 메타데이터.
 * 하나의 Source는 여러 Document(청크)로 쪼개진다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Source {

    /** 소스 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    /** 소속 노트북 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    /** 청크 수 */
    @Column(name = "chunk_count")
    private Integer chunkCount;
}
