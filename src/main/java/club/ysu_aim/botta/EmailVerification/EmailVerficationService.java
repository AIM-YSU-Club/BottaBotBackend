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
import java.util.HexFormat;
import java.util.Random;

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
     * 인증번호를 검증하고 회원의 이메일 인증을 완료한다.
     * 사용 여부를 쓰기 잠금으로 조회하여 동시 요청에서도 일회성 사용을 보장한다.
     *
     * @param randomNum 이메일 링크에서 전달된 원문 토큰
     * @throws EmailVerificationException 토큰이 없거나, 만료되었거나, 이미 사용된 경우
     */
    @Transactional
    public void confirm(String randomNum) {
        if (randomNum == null || randomNum.isBlank()) {
            throw invalidNum();
        }

        Instant now = clock.instant();
        EmailVerification verification = verificationRepository
                .findByTokenHashAndPurpose(randomNum, PURPOSE)
                .orElseThrow(this::invalidNum);

        if (verification.isUsed()) {
            throw new EmailVerificationException(
                    "USED_VERIFICATION_NUM", "이미 사용된 인증 번호입니다.", HttpStatus.CONFLICT);
        }
        if (verification.isExpired(now)) {
            throw new EmailVerificationException(
                    "EXPIRED_VERIFICATION_NUM", "만료된 인증 번호입니다.", HttpStatus.GONE);
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
        String randomNum = generateNum();
        EmailVerification verification = new EmailVerification(
                user, PURPOSE, randomNum, now.plus(Duration.ofMinutes(expirationMinutes)), now);
        verificationRepository.save(verification);
        notifier.sendVerification(user.getEmail(), randomNum);
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
     * @return 인증번호로 사용할 6자리 난수 생성
     */
    private String generateNum() {
        Random random = new Random();
        int randomNumber = random.nextInt(888889) + 111111; // 111111 ~ 999999 범위
        return String.valueOf(randomNumber);
    }

    /** 유효하지 않은 인증 토큰에 사용할 일관된 도메인 예외를 생성한다. */
    private EmailVerificationException invalidNum() {
        return new EmailVerificationException(
                "INVALID_VERIFICATION_Num", "유효하지 않은 인증 번호입니다.", HttpStatus.BAD_REQUEST);
    }
}
