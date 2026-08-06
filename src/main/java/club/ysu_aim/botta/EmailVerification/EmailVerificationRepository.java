package club.ysu_aim.botta.EmailVerification;

import java.util.UUID;
import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByTokenHashAndPurpose(
            String tokenHash, EmailVerificationPurpose purpose);

    Optional<EmailVerification> findTopByUserUserIdAndPurposeOrderByCreatedAtDesc(
            UUID userId, EmailVerificationPurpose purpose);

    long countByUserUserIdAndPurposeAndCreatedAtGreaterThanEqual(
            UUID userId, EmailVerificationPurpose purpose, Instant since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EmailVerification verification
               set verification.usedAt = :usedAt
             where verification.user.userId = :userId
               and verification.purpose = :purpose
               and verification.usedAt is null
            """)
    int markUnusedTokensAsUsed(@Param("userId") UUID userId,
                               @Param("purpose") EmailVerificationPurpose purpose,
                               @Param("usedAt") Instant usedAt);
}
