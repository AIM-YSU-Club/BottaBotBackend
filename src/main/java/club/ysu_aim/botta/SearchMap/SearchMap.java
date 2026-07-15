package club.ysu_aim.botta.SearchMap;

import club.ysu_aim.botta.Chat.Chat;
import club.ysu_aim.botta.Document.Document;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * 질문-검색 다대다 매핑 — LLM 답변의 출처(검색된 Document) 표시용.
 * Chat N : Document M
 */
@Entity
@IdClass(SearchMap.SearchMapId.class)
@Getter
@Setter
@NoArgsConstructor
public class SearchMap {

    /** 대화 ID */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /** 문서 ID */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /** SearchMap 복합 PK */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SearchMapId implements Serializable {
        private UUID chat;
        private UUID document;
    }
}
