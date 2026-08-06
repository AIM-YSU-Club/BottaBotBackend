package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerficationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-06T00:00:00Z");

    @Mock
    private EmailVerificationRepository verificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationNotifier notifier;

    private EmailVerficationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new EmailVerficationService(
                verificationRepository, userRepository, notifier,
                Clock.fixed(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "dailyLimit", 5L);

        user = User.builder()
                .userId(UUID.randomUUID())
                .email("member@example.com")
                .hashedPass("encoded-password")
                .name("member")
                .emailVerified(false)
                .build();
    }

    @Test
    void issueForNewUserStoresOnlyHashAndNotifiesWithRawToken() throws Exception {
        ArgumentCaptor<EmailVerification> entityCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        service.issueForNewUser(user);

        verify(verificationRepository).save(entityCaptor.capture());
        verify(notifier).sendVerification(org.mockito.ArgumentMatchers.eq(user.getEmail()), tokenCaptor.capture());

        EmailVerification saved = entityCaptor.getValue();
        String rawToken = tokenCaptor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(sha256(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));
        assertThat(saved.getPurpose()).isEqualTo(EmailVerificationPurpose.VERIFY_EMAIL);
    }

    @Test
    void confirmMarksUserAndTokenAsVerified() throws Exception {
        String rawToken = "raw-verification-token";
        EmailVerification verification = verification(NOW.plusSeconds(60));
        when(verificationRepository.findByTokenHashAndPurpose(
                sha256(rawToken), EmailVerificationPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(verification));

        service.confirm(rawToken);

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(verification.getUsedAt()).isEqualTo(NOW);
        verify(userRepository).save(user);
        verify(verificationRepository).save(verification);
    }

    @Test
    void confirmRejectsExpiredToken() {
        EmailVerification verification = verification(NOW);
        when(verificationRepository.findByTokenHashAndPurpose(
                any(), org.mockito.ArgumentMatchers.eq(EmailVerificationPurpose.VERIFY_EMAIL)))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.confirm("expired-token"))
                .isInstanceOfSatisfying(EmailVerificationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("EXPIRED_VERIFICATION_TOKEN"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmRejectsAlreadyUsedToken() {
        EmailVerification verification = verification(NOW.plusSeconds(60));
        verification.setUsedAt(NOW.minusSeconds(1));
        when(verificationRepository.findByTokenHashAndPurpose(
                any(), org.mockito.ArgumentMatchers.eq(EmailVerificationPurpose.VERIFY_EMAIL)))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.confirm("used-token"))
                .isInstanceOfSatisfying(EmailVerificationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("USED_VERIFICATION_TOKEN"));
    }

    @Test
    void resendRejectsRequestDuringCooldown() {
        EmailVerification latest = verification(NOW.plusSeconds(60));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationRepository.findTopByUserUserIdAndPurposeOrderByCreatedAtDesc(
                user.getUserId(), EmailVerificationPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.requestVerification(user.getEmail(), true))
                .isInstanceOfSatisfying(EmailVerificationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("VERIFICATION_REQUEST_RATE_LIMITED"));
        verify(notifier, never()).sendVerification(any(), any());
    }

    @Test
    void resendDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestVerification("unknown@example.com", true);

        verify(verificationRepository, never()).save(any());
        verify(notifier, never()).sendVerification(any(), any());
    }

    @Test
    void requestVerificationRequiresTermsAgreement() {
        assertThatThrownBy(() -> service.requestVerification(user.getEmail(), false))
                .isInstanceOfSatisfying(EmailVerificationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_REQUEST"));

        verify(userRepository, never()).findByEmail(any());
        verify(notifier, never()).sendVerification(any(), any());
    }

    private EmailVerification verification(Instant expiresAt) {
        return new EmailVerification(
                user, EmailVerificationPurpose.VERIFY_EMAIL, "stored-hash", expiresAt, NOW);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
