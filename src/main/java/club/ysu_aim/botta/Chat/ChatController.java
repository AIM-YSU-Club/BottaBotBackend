package club.ysu_aim.botta.Chat;

import club.ysu_aim.botta.ChatSession.ChatSessionDTO;
import club.ysu_aim.botta.Security.CustomUserDetails;
import club.ysu_aim.botta.common.ApiEnvelope;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;


@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    /**
     * 대화 세션의 이전 대화 히스토리를 조회하여 맥락 유지용으로 활용
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
            @Parameter(description = "대화 세션 식별자 UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "조회할 최대 대화 턴 수 (기본값: 10)")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        List<ChatSessionDTO.ChatMessageResponse> history = chatService.getMessages(userId, sessionId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 질문을 전송하고 지식베이스 기반의 RAG 스트리밍 답변을 SSE 형식으로 수신함
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
            @Parameter(description = "대화 세션 식별자 UUID", required = true)
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatSessionDTO.SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        return chatService.sendMessageStream(userId, sessionId, request);
    }
}
