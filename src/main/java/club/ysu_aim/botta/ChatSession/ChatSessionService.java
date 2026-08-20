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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 대화 세션 관리 및 RAG 질의응답 비즈니스 로직을 처리하는 서비스 클래스.
 * <p>
 * 대화 세션 생성, 목록 조회, 삭제, 대화 히스토리 조회 및 Server-Sent Events(SSE) 기반의
 * RAG 스트리밍 답변 생성 기능을 제공합니다.
 * 모든 작업은 요청 사용자의 소유권(인가)을 엄격하게 검증합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final NotebookRepository notebookRepository;
    private final ChatRepository chatRepository;
    private final DocumentRepository documentRepository;
    private final SearchMapRepository searchMapRepository;
    private final AnswerDetailRepository answerDetailRepository;

    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    private static final long SSE_TIMEOUT_MS = 180_000L; // 3분 타임아웃
    private static final String DEFAULT_SESSION_TITLE = "새 대화";
    private static final String DEFAULT_MODEL_NAME = "gemini-1.5-pro";

    /**
     * 특정 노트북에 새로운 대화 세션을 생성합니다.
     *
     * @param userId 요청 사용자 식별자 (JWT에서 추출)
     * @param notebookId 소속될 노트북 식별자
     * @return 생성된 대화 세션 식별자와 생성 일시 정보
     * @throws IllegalArgumentException 노트북이 존재하지 않을 경우
     * @throws AccessDeniedException 사용자가 해당 노트북의 소유자가 아닐 경우
     */
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

    /**
     * 지정된 노트북에 속한 모든 대화 세션 목록을 최신 수정일순으로 조회합니다.
     *
     * @param userId 요청 사용자 식별자
     * @param notebookId 대상 노트북 식별자
     * @return 세션 요약 목록 (세션 ID, 제목, 최근 메시지, 수정 일시 등)
     * @throws IllegalArgumentException 노트북이 존재하지 않을 경우
     * @throws AccessDeniedException 사용자가 해당 노트북의 소유자가 아닐 경우
     */
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

    /**
     * 대화 세션의 이전 대화 히스토리를 조회
     * 최대 유지 턴수({@code limit})만큼 최신 대화를 가져온 뒤 시간순(오름차순)으로 정렬하여
     * 사용자 질문과 챗봇 답변을 순서대로 구성합니다. 챗봇 답변에는 연결된 인용 출처(citations)가 포함됩니다.
     *
     *
     */
    public List<ChatSessionDTO.ChatMessageResponse> getMessages(UUID userId, UUID sessionId, int limit) {
        ChatSession session = validateAndGetChatSession(userId, sessionId);
        int validLimit = limit <= 0 ? 10 : limit;

        List<Chat> chatsDesc = chatRepository
                .findByChatSession_ChatSessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, validLimit));

        List<Chat> chats = new ArrayList<>(chatsDesc);
        chats.sort(Comparator.comparing(Chat::getCreatedAt));

        List<ChatSessionDTO.ChatMessageResponse> history = new ArrayList<>();

        for (Chat chat : chats) {
            // 1. 사용자 질문 메시지
            if (chat.getQuestion() != null && !chat.getQuestion().isBlank()) {
                history.add(ChatSessionDTO.ChatMessageResponse.builder()
                        .role("user")
                        .content(chat.getQuestion())
                        .citations(null)
                        .createdAt(chat.getCreatedAt())
                        .build());
            }

            // 2. 챗봇 답변 메시지 및 인용 출처 매핑
            if (chat.getAnswer() != null) {
                List<ChatSessionDTO.CitationDto> citations = getCitationsForChat(chat.getChatId(), session.getNotebook());
                history.add(ChatSessionDTO.ChatMessageResponse.builder()
                        .role("assistant")
                        .content(chat.getAnswer())
                        .citations(citations.isEmpty() ? null : citations)
                        .createdAt(chat.getCreatedAt())
                        .build());
            }
        }

        return history;
    }

    /**
     * 질문을 전송하고 지식베이스 기반의 RAG 스트리밍 답변을 SSE(Server-Sent Events)로 생성합니다.
     * <p>
     * 비동기 작업 스레드에서 토큰 단위 스트리밍을 전송하고 최종 인용 출처를 전달한 후,
     * 대화 내용(Chat), 답변 상세(AnswerDetail), 검색 매핑(SearchMap)을 영속화합니다.
     * </p>
     *
     * @param userId 요청 사용자 식별자
     * @param sessionId 대상 대화 세션 식별자
     * @param request 사용자 질문 요청 DTO
     * @return SSE 스트리밍 Emitter 객체
     * @throws IllegalArgumentException 세션이 존재하지 않을 경우
     * @throws AccessDeniedException 사용자가 해당 세션의 소유자가 아닐 경우
     */
    public SseEmitter sendMessageStream(UUID userId, UUID sessionId, ChatSessionDTO.SendMessageRequest request) {
        ChatSession session = validateAndGetChatSession(userId, sessionId);
        Notebook notebook = session.getNotebook();
        UUID notebookId = notebook.getNotebookId();
        String question = request.getQuestion().trim();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.debug("SSE stream completed for session: {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE stream timed out for session: {}", sessionId);
            emitter.complete();
        });
        emitter.onError(ex -> log.error("SSE stream error for session: {}", sessionId, ex));

        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 1. 노트북에 등록된 문서 청크 및 소스 정보 조회
                List<Document> documents = documentRepository.findByNotebook_NotebookId(notebookId);
                List<Document> matchedDocuments = matchRelevantDocuments(documents, question);

                // 2. 인용 출처(Citations) 구성
                List<ChatSessionDTO.CitationDto> citations = buildCitations(matchedDocuments, notebook);

                // 3. 답변 생성
                String answerText = generateAnswer(question, matchedDocuments, notebook);

                // 4. 답변 텍스트 토큰 단위 스트리밍 전송
                List<String> tokens = splitIntoTokens(answerText);
                for (String token : tokens) {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(Map.of("content", token)));
                    Thread.sleep(25); // 자연스러운 스트리밍 효과를 위한 딜레이
                }

                // 5. 최종 citations 전송
                emitter.send(SseEmitter.event()
                        .name("citations")
                        .data(citations));

                long duration = System.currentTimeMillis() - startTime;

                // 6. DB에 대화 기록 및 메타데이터 저장
                Chat savedChat = saveChatAndMetadata(sessionId, question, answerText, matchedDocuments, duration);

                // 7. 완료(done) 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of(
                                "chatId", savedChat.getChatId().toString(),
                                "createdAt", savedChat.getCreatedAt().toString()
                        )));

                emitter.complete();
            } catch (Exception e) {
                log.error("Error during SSE streaming for session: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", "답변 생성 중 오류가 발생했습니다: " + e.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        }, streamingExecutor);

        return emitter;
    }

    /**
     * 대화(Chat), 답변 상세(AnswerDetail), 검색 매핑(SearchMap)을 트랜잭션 내에서 영속화합니다.
     */
    @Transactional
    public Chat saveChatAndMetadata(UUID sessionId, String question, String answer,
                                    List<Document> matchedDocs, long duration) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화 세션입니다."));

        // 첫 질문일 경우 세션 제목 자동 설정
        if (session.getTitle() == null || session.getTitle().isBlank() || DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            String newTitle = question.length() > 30 ? question.substring(0, 30) + "..." : question;
            session.updateTitle(newTitle);
        } else {
            session.updateTimestamp();
        }

        Chat chat = Chat.builder()
                .chatSession(session)
                .question(question)
                .answer(answer)
                .createdAt(Instant.now())
                .build();

        Chat savedChat = chatRepository.save(chat);

        // AnswerDetail 생성 및 저장
        AnswerDetail answerDetail = AnswerDetail.builder()
                .chat(savedChat)
                .model(DEFAULT_MODEL_NAME)
                .instruction("Notebook 지식베이스에 근거하여 정확하고 요약된 한국어로 답변합니다.")
                .context("참조 청크 수: " + matchedDocs.size())
                .inputTokenCount(question.length() / 2 + matchedDocs.size() * 50)
                .outputTokenCount(answer.length() / 2)
                .inputDuration(duration / 3)
                .outputDuration((duration * 2) / 3)
                .build();

        answerDetailRepository.save(answerDetail);

        // SearchMap 연결 저장
        for (Document doc : matchedDocs) {
            SearchMap searchMap = SearchMap.builder()
                    .chat(savedChat)
                    .document(doc)
                    .build();
            searchMapRepository.save(searchMap);
        }

        return savedChat;
    }

    /**
     * 특정 Chat에 연결된 SearchMap을 기반으로 Citation 목록을 구성합니다.
     */
    private List<ChatSessionDTO.CitationDto> getCitationsForChat(UUID chatId, Notebook notebook) {
        List<SearchMap> searchMaps = searchMapRepository.findByChat_ChatId(chatId);
        List<ChatSessionDTO.CitationDto> citations = new ArrayList<>();

        for (SearchMap sm : searchMaps) {
            Document doc = sm.getDocument();
            if (doc != null) {
                String fileName = findFileNameForNotebook(notebook);
                String snippet = doc.getChunk() != null ? doc.getChunk() : "";
                if (snippet.length() > 150) {
                    snippet = snippet.substring(0, 150) + "...";
                }

                citations.add(ChatSessionDTO.CitationDto.builder()
                        .fileName(fileName)
                        .page(1)
                        .url(null)
                        .snippet(snippet)
                        .build());
            }
        }

        return citations;
    }

    /**
     * 사용자 질문과 관련된 문서 청크를 매칭합니다.
     */
    private List<Document> matchRelevantDocuments(List<Document> documents, String question) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        String[] keywords = question.toLowerCase().split("\\s+");
        List<Document> matched = new ArrayList<>();

        for (Document doc : documents) {
            String chunkText = doc.getChunk();
            if (chunkText != null) {
                String lowerChunk = chunkText.toLowerCase();
                for (String kw : keywords) {
                    if (kw.length() >= 2 && lowerChunk.contains(kw)) {
                        matched.add(doc);
                        break;
                    }
                }
            }
        }

        // 키워드 매칭 결과가 없으면 상위 최대 3개의 청크를 기본 참조로 사용
        if (matched.isEmpty()) {
            return documents.stream().limit(3).toList();
        }

        return matched.stream().limit(5).toList();
    }

    /**
     * 매칭된 문서 청크들로부터 인용 출처(CitationDto) 목록을 생성합니다.
     */
    private List<ChatSessionDTO.CitationDto> buildCitations(List<Document> documents, Notebook notebook) {
        List<ChatSessionDTO.CitationDto> citations = new ArrayList<>();
        String fileName = findFileNameForNotebook(notebook);

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String snippet = doc.getChunk() != null ? doc.getChunk() : "";
            if (snippet.length() > 180) {
                snippet = snippet.substring(0, 180) + "...";
            }

            citations.add(ChatSessionDTO.CitationDto.builder()
                    .fileName(fileName)
                    .page(i + 1)
                    .url(null)
                    .snippet(snippet)
                    .build());
        }

        return citations;
    }

    /**
     * 노트북의 첫 번째 소스 파일명을 추출하거나 기본 파일명을 반환합니다.
     */
    private String findFileNameForNotebook(Notebook notebook) {
        if (notebook != null && notebook.getSources() != null && !notebook.getSources().isEmpty()) {
            for (Source source : notebook.getSources()) {
                if (source.getFiles() != null && !source.getFiles().isEmpty()) {
                    File file = source.getFiles().get(0);
                    if (file.getFileName() != null && !file.getFileName().isBlank()) {
                        return file.getFileName();
                    }
                }
            }
        }
        return (notebook != null && notebook.getTitle() != null) ? notebook.getTitle() + "_문서.pdf" : "참조_문서.pdf";
    }


    /**
     * 스트리밍 전송을 위해 텍스트를 의미 있는 작은 토큰(어절/구문) 단위로 분할합니다.
     */
    private List<String> splitIntoTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        String[] words = text.split("(?<=\\s)|(?<=\n)");
        for (String word : words) {
            tokens.add(word);
        }

        return tokens;
    }

    /**
     * 사용자 소유의 유효한 노트북인지 검증하고 조회합니다.
     */
    private Notebook validateAndGetNotebook(UUID userId, UUID notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트북입니다."));

        if (!notebook.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 노트북에 접근할 권한이 없습니다.");
        }
        return notebook;
    }

    /**
     * 사용자 소유의 유효한 대화 세션인지 검증하고 조회합니다.
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
