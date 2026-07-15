package club.ysu_aim.botta.Chat;

import club.ysu_aim.botta.ChatSession.ChatSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 질문-응답 쌍. AnswerDetail과 1:1, SearchMap을 통해 Document와 N:M.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Chat {

    /** 대화 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    /** 소속 대화 세션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    /** 질문 */
    @Column(columnDefinition = "TEXT")
    private String question;

    /** 챗봇 답변 */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
