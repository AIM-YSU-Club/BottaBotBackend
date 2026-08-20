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


    //세션을 생성 시각 최신순으로 페이징 조회함
    List<Chat> findByChatSession_ChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId, Pageable pageable);

    //대화 세션의 가장 최근 대화 1건을 조회함
    Optional<Chat> findTopByChatSession_ChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId);
}
