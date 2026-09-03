package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 이메일 인증·비밀번호 재설정용 일회성 인증번호.
 * purpose 로 용도를 구분한다.
 */
@Entity
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    /** 인증번호 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "num_id", nullable = false, updatable = false)
    private UUID numId;

    /** 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 인증번호 용도.
     * VERIFY_EMAIL: 이메일 인증, RESET_PASSWORD: 비밀번호 재설정
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "TEXT")
    private EmailVerificationPurpose purpose;

    /** 인증번호 컬럼 */
    @Column(name = "verification_num", nullable = false, columnDefinition = "TEXT")
    private String randomNum;

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

    public EmailVerification(User user, EmailVerificationPurpose purpose, String randomNum,
                             Instant expiresAt, Instant createdAt) {
        this.user = user;
        this.purpose = purpose;
        this.randomNum = randomNum;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
