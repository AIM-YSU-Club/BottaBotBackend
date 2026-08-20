package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Security.CustomUserDetails;
import club.ysu_aim.botta.common.ApiEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * 대화 세션 관리 및 RAG 질의응답 스트리밍 API 컨트롤러.
 * <p>
 * 노트북 단위의 대화 세션 생성, 목록 조회, 삭제와 세션 내 대화 히스토리 조회 및
 * SSE(Server-Sent Events)를 활용한 RAG 스트리밍 답변 생성 엔드포인트를 제공합니다.
 * </p>
 */
@Slf4j
@Tag(name = "대화 세션", description = "노트북 기반 챗봇 세션 관리 및 RAG 메시지 스트리밍 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 지정된 노트북에 새로운 대화 세션을 시작합니다.
     *
     * @param notebookId 대상 노트북 식별자 (UUID)
     * @param userDetails 인증된 사용자 정보 (Spring Security JWT)
     * @return 생성된 대화 세션 ID 및 생성 일시 (201 Created)
     */
    @Operation(
            summary = "새 대화 세션 시작",
            description = "지정된 노트북에 새로운 대화 세션을 생성합니다. 생성된 세션 ID와 생성 시각을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "대화 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = ChatSessionDTO.CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 노트북 ID",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "403", description = "노트북 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/notebooks/{notebookId}/chat-sessions")
    public ResponseEntity<ChatSessionDTO.CreateResponse> createSession(
            @Parameter(description = "노트북 식별자 UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        ChatSessionDTO.CreateResponse response = chatSessionService.createSession(userId, notebookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 지정된 노트북에 속한 대화 세션 목록을 최신 수정일순으로 조회합니다.
     *
     * @param notebookId 대상 노트북 식별자 (UUID)
     * @param userDetails 인증된 사용자 정보
     * @return 대화 세션 요약 목록 (제목, 최근 메시지, 수정일시 포함)
     */
    @Operation(
            summary = "대화 세션 목록 조회",
            description = "지정된 노트북에 속한 모든 대화 세션 목록(제목, 최근 메시지, 수정일시)을 최신 수정일순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 세션 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 노트북 ID",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "403", description = "노트북 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/notebooks/{notebookId}/chat-sessions")
    public ResponseEntity<List<ChatSessionDTO.SessionSummaryResponse>> getSessions(
            @Parameter(description = "노트북 식별자 UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        List<ChatSessionDTO.SessionSummaryResponse> responses = chatSessionService.getSessions(userId, notebookId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 지정된 대화 세션을 삭제합니다.
     *
     * @param sessionId 삭제할 대화 세션 식별자 (UUID)
     * @param userDetails 인증된 사용자 정보
     * @return 204 No Content
     */
    @Operation(
            summary = "대화 세션 삭제",
            description = "지정된 대화 세션과 연관된 대화 내역 및 참조 기록을 완전히 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "대화 세션 삭제 성공 (응답 본문 없음)"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 세션 ID",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/chat-sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "삭제할 대화 세션 식별자 UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        chatSessionService.deleteSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 대화 세션의 이전 대화 히스토리를 조회하여 맥락 유지용으로 활용합니다.
     *
     * @param sessionId 대화 세션 식별자 (UUID)
     * @param limit 조회할 최대 대화 턴 수 (기본값: 10)
     * @param userDetails 인증된 사용자 정보
     * @return 시간순 정렬된 대화 메시지 목록 (role: user/assistant, citations 포함)
     */
    @Operation(
            summary = "대화 히스토리 조회 (맥락 유지용)",
            description = "대화 세션의 이전 대화 내역을 조회합니다. 기본 최대 10턴까지의 질문 및 답변과 인용 출처(citations)를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 히스토리 조회 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 세션 ID",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/chat-sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatSessionDTO.ChatMessageResponse>> getMessages(
            @Parameter(description = "대화 세션 식별자 UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID sessionId,
            @Parameter(description = "조회할 최대 대화 턴 수 (기본값: 10)", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        List<ChatSessionDTO.ChatMessageResponse> history = chatSessionService.getMessages(userId, sessionId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 질문을 전송하고 지식베이스 기반의 RAG 스트리밍 답변을 SSE 형식으로 수신합니다.
     *
     * @param sessionId 대상 대화 세션 식별자 (UUID)
     * @param request 사용자 질문 요청 본문
     * @param userDetails 인증된 사용자 정보
     * @return 답변 토큰 스트림 및 최종 출처(citations)가 전송되는 SseEmitter
     */
    @Operation(
            summary = "질문 전송 및 RAG 답변 스트리밍",
            description = "노트북 지식베이스를 기반으로 질문에 대한 RAG 스트리밍 답변과 최종 인용 출처(파일명·페이지·URL)를 SSE로 스트리밍합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "RAG 스트리밍 답변 전송 시작 (text/event-stream)",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 본문 또는 세션 ID",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping(value = "/chat-sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @Parameter(description = "대화 세션 식별자 UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatSessionDTO.SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        return chatSessionService.sendMessageStream(userId, sessionId, request);
    }
}
