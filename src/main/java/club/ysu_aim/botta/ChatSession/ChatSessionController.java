package club.ysu_aim.botta.ChatSession;


import club.ysu_aim.botta.ChatSession.ChatSessionDTO;
import club.ysu_aim.botta.Security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @GetMapping("/notebooks/{notebookId}/chat-sessions")
    public ResponseEntity<ChatSessionDTO.SearchResponse> searchSession(
            @Valid @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        ChatSessionDTO.SearchResponse response = ChatSessionService.SearchSession(userId, notebookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/notebooks/{notebookId}/chat-sessions")
    public ResponseEntity<ChatSessionDTO.CreateResponse> createSession(
            @Valid @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        ChatSessionDTO.CreateResponse response = ChatSessionService.createSession(userId, notebookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/chat-sessions/{sessionId}")
    public ResponseEntity<Void> DeleteSession(
            @PathVariable UUID chatSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        UUID userId = userDetails.getUserId();

        chatSessionService.deleteChatSession(userId, chatSessionId);

        return ResponseEntity.noContent().build();
    }


}
