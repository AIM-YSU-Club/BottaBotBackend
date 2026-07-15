package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.Notebook.Notebook;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.time.Instant;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NotebookDTO {

    // 1. 노트북 생성 요청
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String title;

        private String description; // 선택 사항
    }

    // 2. 노트북 생성 응답
    @Getter
    @Builder
    public static class CreateResponse {
        private UUID notebookId;
        private Instant createdAt;
    }

    // 3. 노트북 목록 조회 응답 (페이징)
    @Getter
    @Builder
    public static class ListResponse {
        private String title;
        private int sourceCount;
        private Instant updatedAt;
    }

    // 4. 노트북 상세 조회 응답
    @Getter
    @Builder
    public static class DetailResponse {
        private UUID notebookId;
        private String title;
        private String description;
        private Instant createdAt;
        private Instant updatedAt;
        private List<SourceSummaryDto> sources; // 소스 요약 리스트
    }

    // 5. 노트북 수정 요청
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;       // 선택
        private String description; // 선택
    }

    // 소스 요약 정보용 내부 DTO (예시)
    @Getter
    @Builder
    public static class SourceSummaryDto {
        private Long sourceId;
        private String sourceName;
    }
}
