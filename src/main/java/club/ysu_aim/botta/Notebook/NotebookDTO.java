package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.Notebook.Notebook;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.time.Instant;


public class NotebookDTO {

    // 노트북 생성 요청
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String title;

        private String description;
    }

    // 노트북 생성 응답
    @Getter
    @Builder
    public static class CreateResponse {
        private UUID notebookId;
        private Instant createdAt;
    }

    // 노트북 목록 조회 응답 (페이징)
    @Getter
    @Builder
    public static class ListResponse {
        private String title;
        private int sourceCount;
        private Instant updatedAt;
    }

    // 노트북 상세 조회 응답
    @Getter
    @Builder
    public static class DetailResponse {
        private UUID notebookId;
        private String title;
        private String description;
        private Instant createdAt;
        private Instant updatedAt;
        private List<SourceSummaryDto> sources;
    }

    // 노트북 수정 요청
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String description;
    }

    @Getter
    @Builder
    public static class SourceSummaryDto {
        private UUID sourceId;
        private String sourceName;
    }
}
