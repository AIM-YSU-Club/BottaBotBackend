package club.ysu_aim.botta.Source;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 소스(문서 업로드) API의 요청/응답 DTO.
 * 업로드 요청 본문은 multipart file이라 JSON 요청 DTO는 두지 않는다.
 */
public class SourceDTO {
    /**
     * 문서 업로드 수락 응답.
     * AI 서버가 파싱을 끝낸 결과가 아니라, Celery에 넣은 taskId만 담는다.
     */
    @Getter
    @Builder
    @Schema(description = "문서 업로드 수락 응답")
    public static class UploadResponse {
        @Schema(
                description = "AI 서버 Celery 태스크 ID. 파싱 완료 여부는 노트북 소스 목록의 status로 확인한다.",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        )
        private String taskId;
    }
}