package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.AnswerDetail.AnswerDetail;
import club.ysu_aim.botta.AnswerDetail.AnswerDetailRepository;
import club.ysu_aim.botta.Chat.Chat;
import club.ysu_aim.botta.Chat.ChatRepository;
import club.ysu_aim.botta.Document.Document;
import club.ysu_aim.botta.Document.DocumentRepository;
import club.ysu_aim.botta.File.File;
import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Notebook.NotebookRepository;
import club.ysu_aim.botta.SearchMap.SearchMap;
import club.ysu_aim.botta.SearchMap.SearchMapRepository;
import club.ysu_aim.botta.Source.Source;
import club.ysu_aim.botta.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ChatSessionService} 단위 테스트 클래스.
 */
@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private NotebookRepository notebookRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SearchMapRepository searchMapRepository;

    @Mock
    private AnswerDetailRepository answerDetailRepository;

    @InjectMocks
    private ChatSessionService chatSessionService;

    private User owner;
    private User otherUser;
    private Notebook notebook;
    private ChatSession chatSession;
    private UUID notebookId;
    private UUID sessionId;
    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        notebookId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        owner = User.builder()
                .userId(userId)
                .email("owner@example.com")
                .hashedPass("pass")
                .name("Owner")
                .emailVerified(true)
                .build();

        otherUser = User.builder()
                .userId(otherUserId)
                .email("other@example.com")
                .hashedPass("pass")
                .name("Other")
                .emailVerified(true)
                .build();

        notebook = Notebook.builder()
                .notebookId(notebookId)
                .user(owner)
                .title("테스트 노트북")
                .description("테스트 설명")
                .sources(new ArrayList<>())
                .documents(new ArrayList<>())
                .chatSessions(new ArrayList<>())
                .build();

        chatSession = ChatSession.builder()
                .chatSessionId(sessionId)
                .notebook(notebook)
                .title("새 대화")
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .chats(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("새 대화 세션 생성 성공 - 소유자 검증 통과 후 저장")
    void createSession_Success() {
        when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebook));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setChatSessionId(sessionId);
            return session;
        });

        ChatSessionDTO.CreateResponse response = chatSessionService.createSession(userId, notebookId);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getCreatedAt()).isNotNull();
        verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("새 대화 세션 생성 실패 - 존재하지 않는 노트북")
    void createSession_NotebookNotFound() {
        when(notebookRepository.findById(notebookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatSessionService.createSession(userId, notebookId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 노트북입니다.");
    }

    @Test
    @DisplayName("새 대화 세션 생성 실패 - 타인의 노트북 접근 거부 (403)")
    void createSession_AccessDenied() {
        when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebook));

        assertThatThrownBy(() -> chatSessionService.createSession(otherUserId, notebookId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("해당 노트북에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("대화 세션 목록 조회 성공 - 최근 메시지와 함께 반환")
    void getSessions_Success() {
        when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebook));
        when(chatSessionRepository.findByNotebook_NotebookIdAndNotebook_User_UserIdOrderByUpdatedAtDesc(notebookId, userId))
                .thenReturn(List.of(chatSession));

        Chat latestChat = Chat.builder()
                .chatId(UUID.randomUUID())
                .chatSession(chatSession)
                .question("최근 질문입니다")
                .answer("최근 답변입니다")
                .createdAt(Instant.now())
                .build();

        when(chatRepository.findTopByChatSession_ChatSessionIdOrderByCreatedAtDesc(sessionId))
                .thenReturn(Optional.of(latestChat));

        List<ChatSessionDTO.SessionSummaryResponse> responses = chatSessionService.getSessions(userId, notebookId);

        assertThat(responses).hasSize(1);
        ChatSessionDTO.SessionSummaryResponse summary = responses.get(0);
        assertThat(summary.getSessionId()).isEqualTo(sessionId);
        assertThat(summary.getTitle()).isEqualTo("새 대화");
        assertThat(summary.getRecentMessage()).isEqualTo("최근 질문입니다");
    }

    @Test
    @DisplayName("대화 세션 삭제 성공 - 세션 소유자 검증 후 삭제")
    void deleteSession_Success() {
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(chatSession));

        chatSessionService.deleteSession(userId, sessionId);

        verify(chatSessionRepository, times(1)).delete(chatSession);
    }

    @Test
    @DisplayName("대화 세션 삭제 실패 - 타인의 세션 삭제 시도시 AccessDeniedException")
    void deleteSession_AccessDenied() {
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(chatSession));

        assertThatThrownBy(() -> chatSessionService.deleteSession(otherUserId, sessionId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("해당 대화 세션에 접근할 권한이 없습니다.");

        verify(chatSessionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("대화 히스토리 조회 성공 - 질문과 답변이 순서대로 정렬되고 출처가 매핑됨")
    void getMessages_Success() {
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(chatSession));

        UUID chatId = UUID.randomUUID();
        Chat chat1 = Chat.builder()
                .chatId(chatId)
                .chatSession(chatSession)
                .question("AI란 무엇인가요?")
                .answer("AI는 인공지능입니다.")
                .createdAt(Instant.now().minusSeconds(100))
                .build();

        when(chatRepository.findByChatSession_ChatSessionIdOrderByCreatedAtDesc(eq(sessionId), any(PageRequest.class)))
                .thenReturn(List.of(chat1));

        Document doc = new Document();
        doc.setDocumentId(UUID.randomUUID());
        doc.setChunk("인공지능(AI) 개론 내용...");

        SearchMap searchMap = SearchMap.builder()
                .chat(chat1)
                .document(doc)
                .build();

        when(searchMapRepository.findByChat_ChatId(chatId)).thenReturn(List.of(searchMap));

        List<ChatSessionDTO.ChatMessageResponse> history = chatSessionService.getMessages(userId, sessionId, 10);

        assertThat(history).hasSize(2);
        // First message: User
        assertThat(history.get(0).getRole()).isEqualTo("user");
        assertThat(history.get(0).getContent()).isEqualTo("AI란 무엇인가요?");
        assertThat(history.get(0).getCitations()).isNull();

        // Second message: Assistant with Citations
        assertThat(history.get(1).getRole()).isEqualTo("assistant");
        assertThat(history.get(1).getContent()).isEqualTo("AI는 인공지능입니다.");
        assertThat(history.get(1).getCitations()).hasSize(1);
        assertThat(history.get(1).getCitations().get(0).getSnippet()).contains("인공지능(AI) 개론 내용...");
    }

    @Test
    @DisplayName("saveChatAndMetadata 성공 - 첫 질문 시 세션 제목 자동 갱신 및 메타데이터 저장")
    void saveChatAndMetadata_Success() {
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(chatSession));

        Document doc = new Document();
        doc.setDocumentId(UUID.randomUUID());
        doc.setChunk("문서 청크 데이터");

        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat c = invocation.getArgument(0);
            c.setChatId(UUID.randomUUID());
            return c;
        });

        Chat savedChat = chatSessionService.saveChatAndMetadata(
                sessionId, "머신러닝의 지도학습이란?", "지도학습은 라벨이 있는 데이터를 학습합니다.",
                List.of(doc), 1500L
        );

        assertThat(savedChat).isNotNull();
        assertThat(savedChat.getQuestion()).isEqualTo("머신러닝의 지도학습이란?");
        assertThat(chatSession.getTitle()).isEqualTo("머신러닝의 지도학습이란?");

        verify(answerDetailRepository, times(1)).save(any(AnswerDetail.class));
        verify(searchMapRepository, times(1)).save(any(SearchMap.class));
    }

    @Test
    @DisplayName("sendMessageStream 호출시 SseEmitter 정상 반환")
    void sendMessageStream_ReturnsEmitter() {
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(chatSession));
        when(documentRepository.findByNotebook_NotebookId(notebookId)).thenReturn(Collections.emptyList());

        ChatSessionDTO.SendMessageRequest request = ChatSessionDTO.SendMessageRequest.builder()
                .question("테스트 질문")
                .build();

        SseEmitter emitter = chatSessionService.sendMessageStream(userId, sessionId, request);

        assertThat(emitter).isNotNull();
    }
}
