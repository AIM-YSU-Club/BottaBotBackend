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
 */
@Slf4j
@Tag(name = "대화 세션", description = "노트북 기반 챗봇 세션 관리 및 RAG 메시지 스트리밍 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 지정된 노트북에 새로운 대화 세션을 생성
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
            @Parameter(description = "노트북 식별자 UUID", required = true)
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        ChatSessionDTO.CreateResponse response = chatSessionService.createSession(userId, notebookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 지정된 노트북에 속한 대화 세션 목록을 최신 수정일순으로 조회
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
            @Parameter(description = "노트북 식별자 UUID", required = true)
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        List<ChatSessionDTO.SessionSummaryResponse> responses = chatSessionService.getSessions(userId, notebookId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 지정된 대화 세션을 삭제
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
            @Parameter(description = "삭제할 대화 세션 식별자 UUID", required = true)
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();
        chatSessionService.deleteSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
