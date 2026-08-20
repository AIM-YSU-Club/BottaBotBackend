package club.ysu_aim.botta.SearchMap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchMapRepository extends JpaRepository<SearchMap, SearchMap.SearchMapId> {

    /**
     * 특정 대화에 연결된 모든 검색 매핑 목록을 조회함
     */
    List<SearchMap> findByChat_ChatId(UUID chatId);
}
