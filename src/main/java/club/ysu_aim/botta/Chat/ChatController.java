package club.ysu_aim.botta.Chat;

import club.ysu_aim.botta.ChatSession.ChatSession;
import club.ysu_aim.botta.ChatSession.ChatSessionDTO;
import club.ysu_aim.botta.ChatSession.ChatSessionService;
import club.ysu_aim.botta.Notebook.NotebookDTO;
import club.ysu_aim.botta.Security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChatController {

    @GetMapping("/chat-sessions/{sessionId}/messages")

    @PostMapping("/chat-sessions/{sessionId}/messages")
    public ResponseEntity<ChatSession.CreateResponse> createSession(
            @Valid @RequestBody NotebookDTO.CreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        ChatSessionDTO.CreateResponse response = ChatSessionService.createSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
