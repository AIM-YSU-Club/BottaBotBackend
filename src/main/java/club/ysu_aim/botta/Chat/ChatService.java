package club.ysu_aim.botta.Chat;

import club.ysu_aim.botta.ChatSession.ChatSession;
import club.ysu_aim.botta.ChatSession.ChatSessionDTO;
import club.ysu_aim.botta.ChatSession.ChatSessionRepository;
import club.ysu_aim.botta.Document.Document;
import club.ysu_aim.botta.File.File;
import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.SearchMap.SearchMap;
import club.ysu_aim.botta.SearchMap.SearchMapRepository;
import club.ysu_aim.botta.Source.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatRepository chatRepository;
    private final SearchMapRepository searchMapRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final long SSE_TIMEOUT_MS = 180_000L; // 3분 타임아웃
    private static final String DEFAULT_SESSION_TITLE = "새 대화";

    /**
     * 대화 세션의 이전 대화 히스토리를 조회하여 맥락 유지용으로 제공함
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
     * 사용자 입력을 BottaBotAI의 chat_api.py로 전달하고, 반환되는 SSE 스트리밍 응답을 클라이언트에게 중계함
     */
    public SseEmitter sendMessageStream(UUID userId, UUID sessionId, ChatSessionDTO.SendMessageRequest request) {
        ChatSession session = validateAndGetChatSession(userId, sessionId);
        String question = request.getQuestion().trim();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> log.debug("SSE stream 세션 완료: {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE stream 만료됨: {}", sessionId);
            emitter.complete();
        });
        emitter.onError(ex -> log.error("SSE stream 세션 에러: {}", sessionId, ex));

        CompletableFuture.runAsync(() -> {
            try {
                // 1. BottaBotAI 엔드포인트 URL 구성 (POST /chat)
                String baseUrl = aiServerBaseUrl.trim();
                String targetUrl = baseUrl.endsWith("/") ? baseUrl + "chat" : baseUrl + "/chat";

                // 2. multipart/form-data 요청 본문 구성
                String boundary = "----BottaBoundary" + UUID.randomUUID().toString().replace("-", "");
                byte[] multipartBody = buildMultipartBody(boundary, sessionId.toString(), question);

                HttpRequest aiRequest = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .timeout(Duration.ofMillis(SSE_TIMEOUT_MS))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                        .build();

                // 3. BottaBotAI 호출 및 스트림 응답 수신
                HttpResponse<InputStream> aiResponse = httpClient.send(aiRequest, HttpResponse.BodyHandlers.ofInputStream());

                if (aiResponse.statusCode() >= 400) {
                    String errorBody = new String(aiResponse.body().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("BottaBotAI 에러 상태 반환 {}: {}", aiResponse.statusCode(), errorBody);
                    sendErrorEvent(emitter, "AI 서버 응답 오류 (" + aiResponse.statusCode() + "): " + errorBody);
                    emitter.complete();
                    return;
                }

                // 4. SSE 스트림 라인 파싱 및 클라이언트 중계
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(aiResponse.body(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean isDone = false;

                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank() || !line.startsWith("data:")) {
                            continue;
                        }

                        String data = line.substring(5).trim();

                        if ("[DONE]".equals(data)) {
                            isDone = true;
                            break;
                        }

                        try {
                            JsonNode json = objectMapper.readTree(data);
                            if (json.has("token")) {
                                String token = json.get("token").asText();
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(Map.of("content", token)));
                            } else if (json.has("error")) {
                                String errorMsg = json.get("error").asText();
                                sendErrorEvent(emitter, errorMsg);
                            }
                        } catch (Exception parseEx) {
                            log.warn("Failed to parse SSE line data: {}", data, parseEx);
                        }
                    }

                    // 5. 완료 처리 및 세션 메타데이터 / 인용 출처(citations) 전송
                    handleStreamCompletion(session, sessionId, question, emitter);
                }

            } catch (Exception e) {
                log.error("답변 중 오류 발생: {}", sessionId, e);
                sendErrorEvent(emitter, "답변 스트리밍 처리 중 오류가 발생했습니다: " + e.getMessage());
                emitter.completeWithError(e);
            }
        }, streamingExecutor);

        return emitter;
    }

    /**
     * 스트리밍 종료 후 세션 타이틀 갱신, 최신 대화 조회, citations 및 done 이벤트 전송을 처리
     */
    private void handleStreamCompletion(ChatSession session, UUID sessionId, String question, SseEmitter emitter) {
        try {
            // 세션 제목 및 수정일시 업데이트
            updateSessionAfterChat(sessionId, question);

            // BottaBotAI가 저장한 최신 Chat 조회
            Optional<Chat> latestChatOpt = chatRepository.findTopByChatSession_ChatSessionIdOrderByCreatedAtDesc(sessionId);

            if (latestChatOpt.isPresent()) {
                Chat latestChat = latestChatOpt.get();
                List<ChatSessionDTO.CitationDto> citations = getCitationsForChat(latestChat.getChatId(), session.getNotebook());

                // Citations 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("citations")
                        .data(citations));

                // Done 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of(
                                "chatId", latestChat.getChatId().toString(),
                                "createdAt", latestChat.getCreatedAt().toString()
                        )));
            } else {
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("sessionId", sessionId.toString())));
            }

            emitter.complete();
        } catch (Exception e) {
            log.error("세션 완료 실패: {}", sessionId, e);
            emitter.complete();
        }
    }

    /**
     * 대화 완료 후 세션의 제목 및 수정 시간을 업데이트
     */
    @Transactional
    public void updateSessionAfterChat(UUID sessionId, String question) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getTitle() == null || session.getTitle().isBlank() || DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
                String newTitle = question.length() > 30 ? question.substring(0, 30) + "..." : question;
                session.updateTitle(newTitle);
            } else {
                session.updateTimestamp();
            }
            chatSessionRepository.save(session);
        });
    }

    /**
     * multipart/form-data 바이트 배열을 구성
     */
    private byte[] buildMultipartBody(String boundary, String sessionId, String prompt) {
        StringBuilder sb = new StringBuilder();

        // 1. chat_session_id 파라미터
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"chat_session_id\"\r\n\r\n");
        sb.append(sessionId).append("\r\n");

        // 2. prompt 파라미터
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n");
        sb.append(prompt).append("\r\n");

        // 끝 boundary
        sb.append("--").append(boundary).append("--\r\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 에러 SSE 이벤트를 전송
     */
    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message)));
        } catch (IOException e) {
            log.warn("SSE를 통한 오류 전송 실패", e);
        }
    }

    /**
     * 특정 Chat에 연결된 SearchMap을 기반으로 Citation 목록을 구성
     */
    private List<ChatSessionDTO.CitationDto> getCitationsForChat(UUID chatId, Notebook notebook) {
        List<SearchMap> searchMaps = searchMapRepository.findByChat_ChatId(chatId);
        List<ChatSessionDTO.CitationDto> citations = new ArrayList<>();

        int pageNum = 1;
        for (SearchMap sm : searchMaps) {
            Document doc = sm.getDocument();
            if (doc != null) {
                String fileName = resolveFileName(doc, notebook);
                String snippet = doc.getChunk() != null ? doc.getChunk() : "";
                if (snippet.length() > 180) {
                    snippet = snippet.substring(0, 180) + "...";
                }

                citations.add(ChatSessionDTO.CitationDto.builder()
                        .fileName(fileName)
                        .page(pageNum++)
                        .url(null)
                        .snippet(snippet)
                        .build());
            }
        }

        return citations;
    }

    /**
     * Document 엔티티로부터 파일명을 추출하거나 노트북 기본 파일명을 반환
     */
    private String resolveFileName(Document doc, Notebook notebook) {
        if (doc.getSource() != null && doc.getSource().getFiles() != null && !doc.getSource().getFiles().isEmpty()) {
            File file = doc.getSource().getFiles().get(0);
            if (file.getFileName() != null && !file.getFileName().isBlank()) {
                return file.getFileName();
            }
        }
        return findFileNameForNotebook(notebook);
    }

    /**
     * 노트북의 첫 번째 소스 파일명을 추출하거나 기본 파일명을 반환
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
