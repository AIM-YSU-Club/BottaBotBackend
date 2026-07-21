package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.Security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/notebooks")
public class NotebookController {

    private final NotebookService notebookService;

    /**
     * POST /notebooks
     * 노트북 생성
     */
    @PostMapping
    public ResponseEntity<NotebookDTO.CreateResponse> createNotebook(
            @Valid @RequestBody NotebookDTO.CreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        NotebookDTO.CreateResponse response = notebookService.createNotebook(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /notebooks
     * 내 노트북 목록 조회 (최신 수정일 정렬, 페이징, 키워드 검색)
     */
    @GetMapping
    public ResponseEntity<Page<NotebookDTO.ListResponse>> getNotebooks(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        Page<NotebookDTO.ListResponse> response = notebookService.getNotebooks(userId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /notebooks/{notebookId}
     * 노트북 상세 조회 (소스 요약 리스트 포함)
     */
    @GetMapping("/{notebookId}")
    public ResponseEntity<NotebookDTO.DetailResponse> getNotebookDetail(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        NotebookDTO.DetailResponse response = notebookService.getNotebookDetail(userId, notebookId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /notebooks/{notebookId}
     * 노트북 제목/설명 수정
     */
    @PatchMapping("/{notebookId}")
    public ResponseEntity<NotebookDTO.DetailResponse> updateNotebook(
            @PathVariable UUID notebookId,
            @RequestBody NotebookDTO.UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        NotebookDTO.DetailResponse response = notebookService.updateNotebook(userId, notebookId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /notebooks/{notebookId}
     * 노트북 삭제 (소스, 채팅 기록 전체 함께 삭제)
     */
    @DeleteMapping("/{notebookId}")
    public ResponseEntity<Void> deleteNotebook(
            @PathVariable UUID notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getUserId();

        notebookService.deleteNotebook(userId, notebookId);

        return ResponseEntity.noContent().build();
    }
}