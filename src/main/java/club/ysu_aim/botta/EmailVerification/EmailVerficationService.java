package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerficationService {
    private static final EmailVerificationPurpose PURPOSE = EmailVerificationPurpose.VERIFY_EMAIL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailVerificationNotifier notifier;
    private final Clock clock;

    @Value("${email-verification.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${email-verification.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${email-verification.daily-limit:5}")
    private long dailyLimit;

    /**
     * 신규 회원의 최초 이메일 인증 토큰을 발급한다.
     * 재발송 제한은 적용하지 않으며 실제 전송은 notifier 구현체에 위임한다.
     *
     * @param user 회원가입을 완료한 회원
     */
    @Transactional
    public void issueForNewUser(User user) {
        issue(user, false);
    }

    /**
     * 명세의 이메일 인증 발송 요청을 처리한다.
     * 필수 약관 동의를 검증하고 계정 존재 여부가 API 응답으로 노출되지 않도록 없는 이메일은 조용히 종료한다.
     */
    @Transactional
    public void requestVerification(String email, Boolean agreeTerms) {
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(agreeTerms)) {
            throw new EmailVerificationException(
                    "INVALID_REQUEST", "이메일과 필수 약관 동의가 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> issue(user, true));
    }

    /**
     * 원문 토큰을 검증하고 회원의 이메일 인증을 완료한다.
     * 사용 여부를 쓰기 잠금으로 조회하여 동시 요청에서도 일회성 사용을 보장한다.
     *
     * @param rawToken 이메일 링크에서 전달된 원문 토큰
     * @throws EmailVerificationException 토큰이 없거나, 만료되었거나, 이미 사용된 경우
     */
    @Transactional
    public void confirm(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        Instant now = clock.instant();
        EmailVerification verification = verificationRepository
                .findByTokenHashAndPurpose(hash(rawToken), PURPOSE)
                .orElseThrow(this::invalidToken);

        if (verification.isUsed()) {
            throw new EmailVerificationException(
                    "USED_VERIFICATION_TOKEN", "이미 사용된 인증 토큰입니다.", HttpStatus.CONFLICT);
        }
        if (verification.isExpired(now)) {
            throw new EmailVerificationException(
                    "EXPIRED_VERIFICATION_TOKEN", "만료된 인증 토큰입니다.", HttpStatus.GONE);
        }

        User user = verification.getUser();
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
            userRepository.save(user);
        }
        verification.setUsedAt(now);
        verificationRepository.save(verification);
    }

    /**
     * 기존 미사용 토큰을 소진하고 새로운 랜덤 토큰의 해시와 만료 시각을 저장한다.
     *
     * @param user 인증 대상 회원
     * @param enforceLimits 재발송 쿨다운과 일일 제한 적용 여부
     */
    private void issue(User user, boolean enforceLimits) {
        Instant now = clock.instant();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        if (enforceLimits) {
            enforceResendLimits(user, now);
        }

        verificationRepository.markUnusedTokensAsUsed(user.getUserId(), PURPOSE, now);
        String rawToken = generateToken();
        EmailVerification verification = new EmailVerification(
                user, PURPOSE, hash(rawToken), now.plus(Duration.ofMinutes(expirationMinutes)), now);
        verificationRepository.save(verification);
        notifier.sendVerification(user.getEmail(), rawToken);
    }

    /**
     * 마지막 발급 시각과 당일 발급 횟수를 검사해 과도한 재발송 요청을 차단한다.
     *
     * @param user 인증 대상 회원
     * @param now 현재 UTC 시각
     * @throws EmailVerificationException 쿨다운 또는 일일 발급 제한을 초과한 경우
     */
    private void enforceResendLimits(User user, Instant now) {
        verificationRepository.findTopByUserUserIdAndPurposeOrderByCreatedAtDesc(user.getUserId(), PURPOSE)
                .filter(latest -> latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(now))
                .ifPresent(latest -> {
                    throw new EmailVerificationException(
                            "VERIFICATION_REQUEST_RATE_LIMITED",
                            "잠시 후 인증을 다시 요청해주세요.", HttpStatus.TOO_MANY_REQUESTS);
                });

        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        if (verificationRepository.countByUserUserIdAndPurposeAndCreatedAtGreaterThanEqual(
                user.getUserId(), PURPOSE, startOfDay) >= dailyLimit) {
            throw new EmailVerificationException(
                    "VERIFICATION_DAILY_LIMIT_EXCEEDED",
                    "오늘 요청할 수 있는 인증 횟수를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /**
     * URL에 안전하게 포함할 수 있는 256비트 암호학적 랜덤 토큰을 생성한다.
     *
     * @return Base64 URL-safe 형식의 원문 토큰
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 원문 토큰이 유출되지 않도록 SHA-256 해시 문자열로 변환한다.
     *
     * @param token 해싱할 원문 토큰
     * @return 16진수 SHA-256 해시
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** 유효하지 않은 인증 토큰에 사용할 일관된 도메인 예외를 생성한다. */
    private EmailVerificationException invalidToken() {
        return new EmailVerificationException(
                "INVALID_VERIFICATION_TOKEN", "유효하지 않은 인증 토큰입니다.", HttpStatus.BAD_REQUEST);
    }
}
