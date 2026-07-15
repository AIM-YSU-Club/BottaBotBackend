package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.Notebook.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, Long> {
    Optional<Notebook> findByNotebookId(UUID notebookId);

}
