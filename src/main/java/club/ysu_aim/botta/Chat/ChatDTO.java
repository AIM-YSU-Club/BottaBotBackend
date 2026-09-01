package club.ysu_aim.botta.Chat;

import lombok.Builder;
import lombok.Getter;

public class ChatDTO {
    @Getter
    @Builder
    public static class HistoryLimit {
        private int limit;
    }

    @Getter
    @Builder
    public static class History {
        private String role;
        private String content;
        private String citation;
    }

    @Getter
    @Builder
    public static class Messages {
        private String question;
    }
}
