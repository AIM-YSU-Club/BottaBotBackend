package club.ysu_aim.botta.ChatSession;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 대화 세션 및 메시지 송수신에 사용되는 요청/응답 DTO 모음.
 */
public final class ChatSessionDTO {

    private ChatSessionDTO() {
    }

    /**
     * 대화 세션 생성 응답 DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 세션 생성 응답 정보")
    public static class CreateResponse {

        @Schema(description = "생성된 대화 세션 식별자 (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID sessionId;

        @Schema(description = "세션 생성 일시 (UTC)", example = "2026-08-20T08:30:00Z")
        private Instant createdAt;
    }

    /**
     * 대화 세션 목록 조회 요약 응답 DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 세션 요약 목록 응답 정보")
    public static class SessionSummaryResponse {

        @Schema(description = "대화 세션 식별자 (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID sessionId;

        @Schema(description = "대화 세션 제목", example = "인공지능 모델 파인튜닝 가이드")
        private String title;

        @Schema(description = "가장 최근 대화 메시지 내용 (질문 또는 답변)", example = "RAG 파이프라인 구축 절차에 대해 알려줘")
        private String recentMessage;

        @Schema(description = "최종 수정 일시 (UTC)", example = "2026-08-20T08:45:00Z")
        private Instant updatedAt;

        @Schema(description = "생성 일시 (UTC)", example = "2026-08-20T08:30:00Z")
        private Instant createdAt;
    }

    /**
     * 질문 전송 요청 DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "질문 전송 요청 정보")
    public static class SendMessageRequest {

        @NotBlank(message = "질문 내용은 비어 있을 수 없습니다.")
        @Schema(description = "사용자 질문 본문", example = "노트북에 등록된 자료를 바탕으로 요약해줘",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String question;
    }

    /**
     * 인용 출처(Citation) 정보 DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "답변 인용 출처 정보")
    public static class CitationDto {

        @Schema(description = "출처 파일명", example = "deep_learning_handbook.pdf")
        private String fileName;

        @Schema(description = "출처 문서 페이지 번호 (해당되는 경우)", example = "15")
        private Integer page;

        @Schema(description = "출처 웹 URL (웹페이지 소스인 경우)", example = "https://example.com/docs/rag-guide")
        private String url;

        @Schema(description = "참조된 지식 청크 발췌문", example = "RAG는 외부 지식베이스 검색과 생성 모델을 결합하여 환각을 최소화합니다.")
        private String snippet;
    }

    /**
     * 대화 히스토리 단일 메시지 응답 DTO.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 히스토리 메시지 응답 정보")
    public static class ChatMessageResponse {

        @Schema(description = "메시지 발신 주체 (user: 사용자 질문, assistant: 챗봇 답변)", example = "user")
        private String role;

        @Schema(description = "메시지 본문 내용", example = "RAG 시스템에 대해 설명해줘")
        private String content;

        @Schema(description = "인용 출처 목록 (assistant 답변인 경우에만 제공)", nullable = true)
        private List<CitationDto> citations;

        @Schema(description = "메시지 생성 일시 (UTC)", example = "2026-08-20T08:30:00Z")
        private Instant createdAt;
    }
}
