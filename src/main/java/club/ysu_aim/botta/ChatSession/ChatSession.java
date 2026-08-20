package club.ysu_aim.botta.ChatSession;

import club.ysu_aim.botta.Chat.Chat;
import club.ysu_aim.botta.Notebook.Notebook;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 대화 세션(대화창) 엔티티.
 * <p>
 * 하나의 노트북 단위로 여러 개의 대화 세션을 생성하고 관리할 수 있습니다.
 * 대화 세션의 소유자는 소속된 {@link Notebook}의 소유 사용자로 식별됩니다.
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"notebook", "chats"})
public class ChatSession {

    /** 대화 세션 고유 식별자 (UUID PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_session_id", nullable = false, updatable = false)
    private UUID chatSessionId;

    /** 소속 노트북 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    /** 대화 제목 */
    @Column(columnDefinition = "TEXT")
    private String title;

    /** 생성 시각 */
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();

    /** 최종 수정 시각 */
    @Builder.Default
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant updatedAt = Instant.now();

    /** 대화 목록 (질문-답변 쌍 턴 목록) */
    @Builder.Default
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> chats = new ArrayList<>();

    /**
     * 대화 세션의 제목을 갱신하고 수정 시각을 현재 시각으로 업데이트합니다.
     *
     * @param title 새 대화 세션 제목
     */
    public void updateTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
        this.updatedAt = Instant.now();
    }

    /**
     * 대화 세션의 수정 시각을 현재 시각으로 갱신합니다.
     */
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
