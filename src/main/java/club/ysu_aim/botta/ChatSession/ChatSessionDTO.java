package club.ysu_aim.botta.ChatSession;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public class ChatSessionDTO {

    // 노트북 생성 요청
    @Getter
    @NoArgsConstructor
    public static class SessionRequest {
        private UUID notebookId;
    }

    // 노트북 생성 응답
    @Getter
    @Builder
    public static class CreateResponse {
        private UUID sessionId;
        private Instant createdAt;
    }
    @Getter
    @Builder
    public static class SearchResponse {
        private String tilte;
        private String content;
        private Instant createdAt;
    }

    @Getter
    @Builder
    public static class DeleteSession {
        private UUID sessionId;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private UUID sessionId;
        private String title;
        private List chats;
        private Instant updatedAt;
    }
}
