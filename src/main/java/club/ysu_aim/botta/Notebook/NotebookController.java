package club.ysu_aim.botta.Notebook;


import club.ysu_aim.botta.Security.CustomUserDetails;
import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Notebook.NotebookService;
import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
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

        // Security Context에서 토큰을 통해 인증된 유저 ID를 추출
        User userId = userDetails.getUserId();

        NotebookDTO.CreateResponse response = notebookService.createNotebook(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /notebooks
     * 내 노트북 목록 조회 (최신 수정일 정렬, 페이징, 키워드 검색)
     */
    @GetMapping
    public ResponseEntity<Page<NotebookDto.ListResponse>> getNotebooks(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        Page<NotebookDto.ListResponse> response = notebookService.getNotebooks(userId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /notebooks/{notebookId}
     * 노트북 상세 조회 (소스 요약 리스트 포함)
     */
    @GetMapping("/{notebookId}")
    public ResponseEntity<NotebookDto.DetailResponse> getNotebookDetail(
            @PathVariable Long notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        NotebookDto.DetailResponse response = notebookService.getNotebookDetail(userId, notebookId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /notebooks/{notebookId}
     * 노트북 제목/설명 수정
     */
    @PatchMapping("/{notebookId}")
    public ResponseEntity<NotebookDto.DetailResponse> updateNotebook(
            @PathVariable Long notebookId,
            @RequestBody NotebookDto.UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        // 수정된 정보를 반환하도록 DetailResponse 재사용
        NotebookDto.DetailResponse response = notebookService.updateNotebook(userId, notebookId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /notebooks/{notebookId}
     * 노트북 삭제 (소스, 채팅 기록 전체 함께 삭제)
     */
    @DeleteMapping("/{notebookId}")
    public ResponseEntity<Void> deleteNotebook(
            @PathVariable Long notebookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        notebookService.deleteNotebook(userId, notebookId);

        // 명세서 요구사항: 204 No Content 반환
        return ResponseEntity.noContent().build();
    }
}