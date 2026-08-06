package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;

    @Transactional
    public NotebookDTO.CreateResponse createNotebook(UUID userId, NotebookDTO.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Notebook notebook = Notebook.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Notebook savedNotebook = notebookRepository.save(notebook);

        return NotebookDTO.CreateResponse.builder()
                .notebookId(savedNotebook.getNotebookId())
                .createdAt(savedNotebook.getCreatedAt())
                .build();
    }

    public Page<NotebookDTO.ListResponse> getNotebooks(UUID userId, String keyword, Pageable pageable) {
        Page<Notebook> notebooks = (keyword == null || keyword.isEmpty()) ?
                notebookRepository.findByUser_UserId(userId, pageable) :
                notebookRepository.findByUser_UserIdAndTitleContaining(userId, keyword, pageable);

        return notebooks.map(notebook -> NotebookDTO.ListResponse.builder()
                .title(notebook.getTitle())
                .sourceCount(notebook.getSources().size())
                .updatedAt(notebook.getUpdatedAt())
                .build());
    }

    public NotebookDTO.DetailResponse getNotebookDetail(UUID userId, UUID notebookId) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        List<NotebookDTO.SourceSummaryDto> sources = notebook.getSources().stream()
                .map(source -> {
                    String sourceName = (source.getFiles() != null && !source.getFiles().isEmpty())
                            ? source.getFiles().get(0).getFileName()
                            : "Unknown Source";
                    return NotebookDTO.SourceSummaryDto.builder()
                            .sourceId(source.getSourceId())
                            .sourceName(sourceName)
                            .build();
                })
                .toList();

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
    public NotebookDTO.DetailResponse updateNotebook(UUID userId, UUID notebookId, NotebookDTO.UpdateRequest request) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);

        notebook.update(request.getTitle(), request.getDescription());

        return getNotebookDetail(userId, notebookId);
    }

    @Transactional
    public void deleteNotebook(UUID userId, UUID notebookId) {
        Notebook notebook = validateAndGetNotebook(userId, notebookId);
        notebookRepository.delete(notebook);
    }

    private Notebook validateAndGetNotebook(UUID userId, UUID notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트북입니다."));

        if (!notebook.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("해당 노트북에 접근할 권한이 없습니다.");
        }
        return notebook;
    }
}
