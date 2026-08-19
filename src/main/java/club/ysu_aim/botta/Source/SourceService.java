package club.ysu_aim.botta.Source;

import club.ysu_aim.botta.Notebook.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceService {
    // 허용할 파일 형식
    private static final Set<String> ALLOWED = Set.of(".pdf", ".docx", ".pptx", ".xlsx");

    private final NotebookService notebookService; // 또는 NotebookRepository
    private final AiDocparseClient aiDocparseClient;

    public SourceDTO.UploadResponse upload(UUID userId, UUID notebookId, MultipartFile file) {
        // 노트북 조회
        notebookService.validateAndGetNotebook(userId, notebookId); // 지금은 private → public/package로 열기
        // 파일 무결성 검사
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        // 파일명 추출
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        // 파일 형식 추출
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        // 허용하는 형식인지 검사
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + ext);
        }
        // 업로드 요청 후 즉시 task_id 반환
        String taskId = aiDocparseClient.upload(notebookId, file);
        return SourceDTO.UploadResponse.builder().taskId(taskId).build();
    }
}