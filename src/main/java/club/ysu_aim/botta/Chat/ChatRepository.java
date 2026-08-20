package club.ysu_aim.botta.Chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 대화(질문-답변 턴) 엔티티에 대한 데이터 액세스 리포지토리 인터페이스.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * 대화 세션에 속한 모든 대화를 생성 시각 오름차순으로 조회합니다.
     *
     * @param chatSessionId 대화 세션 식별자
     * @return 시간순 대화 목록
     */
    List<Chat> findByChatSession_ChatSessionIdOrderByCreatedAtAsc(UUID chatSessionId);

    /**
     * 대화 세션에 속한 대화를 생성 시각 내림차순(최신순)으로 페이징 조회합니다.
     *
     * @param chatSessionId 대화 세션 식별자
     * @param pageable 페이징 정보
     * @return 최신순 대화 목록
     */
    List<Chat> findByChatSession_ChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId, Pageable pageable);

    /**
     * 대화 세션의 가장 최근 대화 1건을 조회합니다.
     *
     * @param chatSessionId 대화 세션 식별자
     * @return 가장 최근 대화 Optional 객체
     */
    Optional<Chat> findTopByChatSession_ChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId);
}
