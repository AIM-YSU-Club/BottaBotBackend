package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 이메일 인증·비밀번호 재설정용 일회성 토큰.
 * JWT/액세스 토큰과 무관하며, purpose 로 용도를 구분한다.
 */
@Entity
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    /** 토큰 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id", nullable = false, updatable = false)
    private UUID tokenId;

    /** 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 토큰 용도.
     * VERIFY_EMAIL: 이메일 인증, RESET_PASSWORD: 비밀번호 재설정
     */
    @Column(
            nullable = false,
            columnDefinition = "TEXT CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))"
    )
    private String purpose;

    /** 토큰 값(원문 대신 해시 저장 권장) */
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    /** 만료 시각 */
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant expiresAt;

    /** 사용 완료 시각 (null이면 미사용) */
    @Column(name = "used_at", columnDefinition = "TIMESTAMPTZ")
    private Instant usedAt;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
