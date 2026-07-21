package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.User.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * 노트북 — RAG 챗봇의 기본 단위(지식베이스/프로젝트).
 */
@Entity
@Builder
@AllArgsConstructor
@ToString(exclude = {"sources", "chatSessions", "documents"})
@Getter
@Setter
@NoArgsConstructor
public class Notebook {

    /** 노트북 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notebook_id", nullable = false, updatable = false)
    private UUID notebookId;

    /** 소유 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 노트북 제목 */
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 생성 시각 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();

    /** 수정 시각 */
    @Builder.Default
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant updatedAt = Instant.now();

    /** 소스 목록 */
    @Builder.Default
    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<club.ysu_aim.botta.Source.Source> sources = new ArrayList<>();

    /** 대화 세션 목록 */
    @Builder.Default
    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<club.ysu_aim.botta.ChatSession.ChatSession> chatSessions = new ArrayList<>();

    /** 문서 청크 목록 */
    @Builder.Default
    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<club.ysu_aim.botta.Document.Document> documents = new ArrayList<>();

    public void update(String title, String description) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = Instant.now();
    }
}
