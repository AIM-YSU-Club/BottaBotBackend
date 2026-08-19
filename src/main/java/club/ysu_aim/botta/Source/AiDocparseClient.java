package club.ysu_aim.botta.Source;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiDocparseClient {

    private final RestClient aiRestClient;

    public String upload(UUID notebookId, MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("notebook_id", notebookId.toString());
        builder.part("file", file.getResource())
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType() != null
                        ? MediaType.parseMediaType(file.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM);

        AiTaskResponse response = aiRestClient.post()
                .uri("/docparse/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("AI 업로드 실패: " + res.getStatusCode());
                })
                .body(AiTaskResponse.class);

        if (response == null || response.taskId() == null) {
            throw new IllegalStateException("AI 서버가 task_id를 반환하지 않았습니다.");
        }
        return response.taskId();
    }

    public record AiTaskResponse(@JsonProperty("task_id") String taskId) {}
}