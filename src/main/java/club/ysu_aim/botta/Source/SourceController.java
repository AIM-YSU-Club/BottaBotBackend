package club.ysu_aim.botta.Source;

import club.ysu_aim.botta.Security.CustomUserDetails;
import club.ysu_aim.botta.common.ApiEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notebooks")
public class SourceController {
    private final SourceService sourceService;

    @Operation(summary = "문서 업로드", description = "파일을 AI 서버로 전달하고 Celery task_id를 반환합니다.")
    @PostMapping(value = "/{notebookId}/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SourceDTO.UploadResponse> upload(
        @PathVariable UUID notebookId,
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserDetails user
    )
    {
        SourceDTO.UploadResponse body = sourceService.upload(user.getUserId(), notebookId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}