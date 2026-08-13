//package club.ysu_aim.botta.ChatSession;
//
//import club.ysu_aim.botta.Notebook.NotebookDTO;
//import club.ysu_aim.botta.Security.CustomUserDetails;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@Slf4j
//@RequiredArgsConstructor
//@RestController
//public class ChatSessionController {
//
//    private final ChatSessionService chatSessionService;
//
//    @DeleteMapping("/chat-sessions/{sessionId}")
//    public ResponseEntity<Void> deleteChatSession(
//            @PathVariable UUID chatSessionId,
//            @AuthenticationPrincipal CustomUserDetails userDetails){
//
//        UUID userId = userDetails.getUserId();
//
//        chatSessionService.deleteChatSession(userId, sessionId);
//
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/chat-sessions/{sessionId}/messages")
//
//    @PostMapping("/chat-sessions/{sessionId}/messages")
//    public ResponseEntity<ChatSession.CreateResponse> createChatSession(
//            @Valid @RequestBody NotebookDTO.CreateRequest request,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        UUID userId = userDetails.getUserId();
//
//        ChatSessionDTO.CreateResponse response = ChatSessionService.createChatSession(userId, request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//}
