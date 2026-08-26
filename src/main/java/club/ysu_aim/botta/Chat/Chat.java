package club.ysu_aim.botta.Chat;

import club.ysu_aim.botta.ChatSession.ChatSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 질문-답변 대화 턴 엔티티.
 * <p>
 * {@link ChatSession}에 소속되며, 사용자 질문({@code question})과 챗봇 답변({@code answer})을 보관합니다.
 * LLM 메타데이터는 AnswerDetail과 1:1 관계를 맺고, RAG 참조 문서는 SearchMap을 통해 Document와 N:M 관계를 맺습니다.
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"chatSession"})
public class Chat {

    /** 대화 ID (UUID PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    /** 소속 대화 세션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    /** 사용자 질문 내용 */
    @Column(columnDefinition = "TEXT")
    private String question;

    /** 챗봇 답변 내용 */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** 대화 생성 시각 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
