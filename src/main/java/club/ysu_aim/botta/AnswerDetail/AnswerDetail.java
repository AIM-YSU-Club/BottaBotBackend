package club.ysu_aim.botta.AnswerDetail;

import club.ysu_aim.botta.Chat.Chat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 챗봇 답변 상세 — Chat과 1:1 (공유 PK = chat_id).
 * LLM 모델·토큰·처리시간 등 응답 생성 메타데이터를 보관한다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AnswerDetail {

    /** 대화 ID (Chat과 공유 PK) */
    @Id
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "chat_id")
    private Chat chat;

    /** 응답 생성에 사용된 LLM 모델 */
    @Column(columnDefinition = "TEXT")
    private String model;

    /** 이전 대화 맥락(대화 히스토리) */
    @Column(columnDefinition = "TEXT")
    private String context;

    /** 응답 생성 지침 */
    @Column(columnDefinition = "TEXT")
    private String instruction;

    /** 입력 토큰 수 */
    @Column(name = "input_token_count")
    private Integer inputTokenCount;

    /** 출력 토큰 수 */
    @Column(name = "output_token_count")
    private Integer outputTokenCount;

    /** 입력 처리 시간(ms) */
    @Column(name = "input_duration")
    private Long inputDuration;

    /** 출력 처리 시간(ms) */
    @Column(name = "output_duration")
    private Long outputDuration;
}
