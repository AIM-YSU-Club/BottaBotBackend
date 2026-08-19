package club.ysu_aim.botta.Source;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * BottaBotAI 문서 파싱 업로드 클라이언트.
 */
@Component
@RequiredArgsConstructor
public class AiDocparseClient {

    private final RestClient aiRestClient;

    /**
     * AI {@code POST /docparse/upload}를 호출하고 Celery task_id를 반환한다.
     * Content-Type을 {@code multipart/form-data}로 직접 넣으면 boundary가 빠져
     * FastAPI가 part를 읽지 못하고 422를 반환하므로, MultipartBodyBuilder가
     * boundary를 붙이도록 헤더는 설정하지 않는다.
     *
     * @param notebookId 업로드 대상 노트북 ID
     * @param file 원본 파일
     * @return Celery task_id
     */
    public String upload(UUID notebookId, MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("notebook_id", notebookId.toString())
                .contentType(MediaType.TEXT_PLAIN);
        builder.part("file", file.getResource())
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType() != null
                        ? MediaType.parseMediaType(file.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM);

        AiTaskResponse response = aiRestClient.post()
                .uri("/docparse/upload")
                .body(builder.build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException(
                            "AI 업로드 실패: " + res.getStatusCode() + " " + errorBody);
                })
                .body(AiTaskResponse.class);

        if (response == null || response.taskId() == null) {
            throw new IllegalStateException("AI 서버가 task_id를 반환하지 않았습니다.");
        }
        return response.taskId();
    }

    /** AI /docparse/upload 응답. 프론트 DTO가 아니다. */
    public record AiTaskResponse(@JsonProperty("task_id") String taskId) {}
}