package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotebookService {

    private final NotebookRepository notebookRepository;


    @Transactional
    public NotebookDTO.CreateResponse createNotebook(User user, NotebookDTO.CreateRequest request) {
        Notebook notebook = Notebook.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Notebook savedNotebook = notebookRepository.save(notebook);

        return NotebookDTO.CreateResponse.builder()
                .notebookId(savedNotebook.getId())
                .createdAt(savedNotebook.getCreatedAt())
                .build();
    }


    public Page<NotebookDTO.ListResponse> getNotebooks(Long userId, String keyword, Pageable pageable) {
        // QueryDSL이나 Spring Data JPA로 keyword 검색 조건 처리
        // 예시: findByUserIdAndTitleContaining(userId, keyword, pageable)
        Page<Notebook> notebooks = (keyword == null || keyword.isEmpty()) ?
                notebookRepository.findByUserId(userId, pageable) :
                notebookRepository.findByUserIdAndTitleContaining(userId, keyword, pageable);

        return notebooks.map(notebook -> NotebookDTO.ListResponse.builder()
                .title(notebook.getTitle())
                .sourceCount(0) // TODO: 연관된 소스 엔티티의 개수를 카운트하는 로직 추가 필요
                .updatedAt(notebook.getUpdatedAt())
                .build());
    }


    public NotebookDTO.DetailResponse getNotebookDetail(Long userId, UUID notebookId) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        // TODO: 소스 엔티티 리스트 조회 로직 구현
        List<NotebookDTO.SourceSummaryDto> sources = new ArrayList<>();

        return NotebookDTO.DetailResponse.builder()
                .notebookId(notebook.getNotebookId())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .createdAt(notebook.getCreatedAt())
                .updatedAt(notebook.getUpdatedAt())
                .sources(sources)
                .build();
    }


    @Transactional
    public NotebookDTO.DetailResponse updateNotebook(Long userId, Long notebookId, NotebookDTO.UpdateRequest request) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        // 더티 체킹(Dirty Checking)을 통한 업데이트
        notebook.update(request.getTitle(), request.getDescription());

        return getNotebookDetail(userId, notebookId); // 업데이트된 상세 정보 반환
    }


    @Transactional
    public void deleteNotebook(Long userId, Long notebookId) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        // CascadeType.ALL 혹은 별도 쿼리를 통해 소스/채팅 기록 삭제 로직 필요
        // sourceRepository.deleteByNotebookId(notebookId);
        // chatSessionRepository.deleteByNotebookId(notebookId);

        notebookRepository.delete(notebook);
    }

    /**
     * 공통 검증 로직: 노트북이 존재하는지, 현재 요청한 유저의 소유인지 확인
     */
    private Notebook validateAndGetNotebook(Long userId, Long notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트북입니다."));

        if (!notebook.getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 노트북에 접근할 권한이 없습니다.");
        }
        return notebook;
    }
}
