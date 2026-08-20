package club.ysu_aim.botta.ChatSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 대화 세션(ChatSession) 엔티티에 대한 데이터 액세스 리포지토리 인터페이스.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * 대화 세션 식별자로 세션을 조회합니다.
     *
     * @param chatSessionId 대화 세션 식별자
     * @return 대화 세션 Optional 객체
     */
    Optional<ChatSession> findByChatSessionId(UUID chatSessionId);

    /**
     * 특정 노트북에 속한 모든 대화 세션 목록을 최신 수정일순으로 조회합니다.
     *
     * @param notebookId 노트북 식별자
     * @return 최신 수정일순 대화 세션 목록
     */
    List<ChatSession> findByNotebook_NotebookIdOrderByUpdatedAtDesc(UUID notebookId);

    /**
     * 대화 세션 식별자와 소유 사용자 식별자로 대화 세션을 조회합니다.
     *
     * @param chatSessionId 대화 세션 식별자
     * @param userId 소유 사용자 식별자
     * @return 검증된 대화 세션 Optional 객체
     */
    Optional<ChatSession> findByChatSessionIdAndNotebook_User_UserId(UUID chatSessionId, UUID userId);

    /**
     * 특정 노트북과 소유 사용자에 속한 모든 대화 세션 목록을 최신 수정일순으로 조회합니다.
     *
     * @param notebookId 노트북 식별자
     * @param userId 소유 사용자 식별자
     * @return 최신 수정일순 대화 세션 목록
     */
    List<ChatSession> findByNotebook_NotebookIdAndNotebook_User_UserIdOrderByUpdatedAtDesc(UUID notebookId, UUID userId);
}
