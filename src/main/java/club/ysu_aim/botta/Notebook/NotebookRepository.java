package club.ysu_aim.botta.Notebook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
    Optional<Notebook> findByNotebookId(UUID notebookId);
    Page<Notebook> findByUser_UserId(UUID userId, Pageable pageable);
    Page<Notebook> findByUser_UserIdAndTitleContaining(UUID userId, String keyword, Pageable pageable);
}
