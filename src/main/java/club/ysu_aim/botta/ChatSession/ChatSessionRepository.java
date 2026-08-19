package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Notebook.Notebook;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByNotebookId(UUID sessionId);
}
