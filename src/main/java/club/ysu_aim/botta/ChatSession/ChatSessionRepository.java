package club.ysu_aim.botta.ChatSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {


     //세션 목록을 최신 수정일순으로 조회함
    List<ChatSession> findByNotebook_NotebookIdAndNotebook_User_UserIdOrderByUpdatedAtDesc(UUID notebookId, UUID userId);
}
