package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Chat.Chat;
import club.ysu_aim.botta.Chat.ChatRepository;
import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Notebook.NotebookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/**
 * 대화 세션 관리 및 BottaBotAI RAG 연동 비즈니스 로직을 처리하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final NotebookRepository notebookRepository;
    private final ChatRepository chatRepository;

    @Value("${ai.service.url:http://localhost:8000}")

    private static final String DEFAULT_SESSION_TITLE = "새 대화";


    @Transactional
    public ChatSessionDTO.CreateResponse createSession(UUID userId, UUID notebookId) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        ChatSession chatSession = ChatSession.builder()
                .notebook(notebook)
                .title(DEFAULT_SESSION_TITLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatSession savedSession = chatSessionRepository.save(chatSession);

        return ChatSessionDTO.CreateResponse.builder()
                .sessionId(savedSession.getChatSessionId())
                .createdAt(savedSession.getCreatedAt())
                .build();
    }


    public List<ChatSessionDTO.SessionSummaryResponse> getSessions(UUID userId, UUID notebookId) {
        validateAndGetNotebook(userId, notebookId);

        List<ChatSession> sessions = chatSessionRepository
                .findByNotebook_NotebookIdAndNotebook_User_UserIdOrderByUpdatedAtDesc(notebookId, userId);

        return sessions.stream().map(session -> {
            Optional<Chat> latestChat = chatRepository
                    .findTopByChatSession_ChatSessionIdOrderByCreatedAtDesc(session.getChatSessionId());

            String recentMessage = latestChat.map(chat -> {
                if (chat.getQuestion() != null && !chat.getQuestion().isBlank()) {
                    return chat.getQuestion();
                } else if (chat.getAnswer() != null && !chat.getAnswer().isBlank()) {
                    return chat.getAnswer();
                }
                return null;
            }).orElse(null);

            return ChatSessionDTO.SessionSummaryResponse.builder()
                    .sessionId(session.getChatSessionId())
                    .title(session.getTitle() != null ? session.getTitle() : DEFAULT_SESSION_TITLE)
                    .recentMessage(recentMessage)
                    .updatedAt(session.getUpdatedAt())
                    .createdAt(session.getCreatedAt())
                    .build();
        }).toList();
    }


    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        ChatSession chatSession = validateAndGetChatSession(userId, sessionId);
        chatSessionRepository.delete(chatSession);
    }
    private Notebook validateAndGetNotebook(UUID userId, UUID notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트북입니다."));

        if (!notebook.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 노트북에 접근할 권한이 없습니다.");
        }
        return notebook;
    }
    /**
     * 사용자 소유의 유효한 대화 세션인지 검증하고 조회
     */
    private ChatSession validateAndGetChatSession(UUID userId, UUID sessionId) {
        ChatSession chatSession = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화 세션입니다."));

        if (!chatSession.getNotebook().getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 대화 세션에 접근할 권한이 없습니다.");
        }
        return chatSession;
    }
}
