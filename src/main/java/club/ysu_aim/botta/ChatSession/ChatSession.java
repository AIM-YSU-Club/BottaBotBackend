package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Notebook.Notebook;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 대화 세션(대화창). 노트북 단위로 여러 세션을 가질 수 있다.
 * 소유 사용자는 notebook → user 경로로 식별한다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    /** 대화 세션 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_session_id", nullable = false, updatable = false)
    private UUID chatSessionId;

    /** 소속 노트북 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    /** 대화 제목 */
    @Column(columnDefinition = "TEXT")
    private String title;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
