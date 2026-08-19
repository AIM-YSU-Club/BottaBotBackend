package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Notebook.NotebookDTO;
import club.ysu_aim.botta.Security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @DeleteMapping("/chat-sessions/{sessionId}")
    public ResponseEntity<Void> deleteChatSession(
            @PathVariable UUID chatSessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        UUID userId = userDetails.getUserId();

        chatSessionService.deleteChatSession(userId, chatSessionId);

        return ResponseEntity.noContent().build();
    }


}
