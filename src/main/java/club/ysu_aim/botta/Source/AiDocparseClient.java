package club.ysu_aim.botta.Source;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
     * {@code MultiValueMap}에 파일 {@code Resource}를 넣어야
     * {@code FormHttpMessageConverter}가 {@code multipart/form-data}와 boundary를 붙인다.
     * Jackson이 맵을 JSON으로 직렬화하지 않도록 Content-Type을 multipart로 지정한다.
     *
     * @param notebookId 업로드 대상 노트북 ID
     * @param file 원본 파일
     * @return Celery task_id
     */
    public String upload(UUID notebookId, MultipartFile file) {
        ByteArrayResource filePart = toNamedResource(file);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("notebook_id", notebookId.toString());
        parts.add("file", filePart);

        AiTaskResponse response = aiRestClient.post()
                .uri("/docparse/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
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

    /**
     * FastAPI {@code UploadFile.filename}과 Docling 확장자 판별에 파일명이 필요하다.
     */
    private static ByteArrayResource toNamedResource(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다.", e);
        }
        String filename = file.getOriginalFilename();
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    /** AI /docparse/upload 응답. 프론트 DTO가 아니다. */
    public record AiTaskResponse(@JsonProperty("task_id") String taskId) {}
}
