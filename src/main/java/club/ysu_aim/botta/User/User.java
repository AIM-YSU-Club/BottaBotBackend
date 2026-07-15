package club.ysu_aim.botta.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 계정.
 * PostgreSQL 예약어 충돌을 피하기 위해 테이블명은 users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    /** 로그인 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** 로그인 이메일 */
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String email;

    /** 해시된 비밀번호 */
    @Column(name = "hashed_pass", nullable = false, columnDefinition = "TEXT")
    private String hashedPass;

    /** 이름 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    /** 학번 */
    @Column(name = "student_id", unique = true, columnDefinition = "TEXT")
    private String studentId;

    /** 별명 */
    @Column(columnDefinition = "TEXT")
    private String nickname;

    /** 이메일 인증 완료 여부 */
    @Column(name = "email_verified", nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean emailVerified = false;

    /** 이메일 인증 완료 시각 */
    @Column(name = "email_verified_at", columnDefinition = "TIMESTAMPTZ")
    private Instant emailVerifiedAt;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt = Instant.now();
}
