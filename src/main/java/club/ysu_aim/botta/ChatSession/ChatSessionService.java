package club.ysu_aim.botta.ChatSession;


import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Notebook.NotebookDTO;
import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionDTO.CreateResponse createSession(UUID userId, ChatSessionDTO.SessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ChatSession chatSession = ChatSession.builder()
                .user(user)
                .notebookId(request.getNotebookId())
                .build();

        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        return ChatSessionDTO.CreateResponse.builder()
                .sessionId(savedChatSession.getChatSessionId())
                .createdAt(savedChatSession.getCreatedAt())
                .build();
    }

    public ChatSessionDTO.ListResponse SearchSession(UUID userId, String notebookId) {
        ChatSession sessions = chatSessionRepository.findByUser_UserId(userId, notebookId);


        return sessions.map(chatSession -> ChatSessionDTO.ListResponse.builder()
                .sessionId(sessions.getChatSessionId())
                .title(sessions.getTitle())
                .chats(sessions.getChats())
                .updatedAt(sessions.getUpdatedAt())
                .build());
    }
    @Transactional
    public void deleteChatSession(UUID userId, UUID sessionId) {
        ChatSession chatSession = validateAndGetChatSession(userId, sessionId);
        ChatSessionRepository.delete(chatSession);
    }
    private ChatSession validateAndGetChatSession(UUID userId, UUID sessionId) {
        ChatSession chatSession = ChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트북입니다."));

        if (!ChatSession.getChatSessionId().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 노트북에 접근할 권한이 없습니다.");
        }
        return notebook;
    }
}
