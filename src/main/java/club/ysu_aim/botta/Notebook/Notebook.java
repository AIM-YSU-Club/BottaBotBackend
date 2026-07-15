package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 노트북 — RAG 챗봇의 기본 단위(지식베이스/프로젝트).
 */
@Entity
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
    @Column(name = "notebook_name", columnDefinition = "TEXT")
    private String notebookName;
}
