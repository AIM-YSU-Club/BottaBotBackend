package club.ysu_aim.botta.EmailVerification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GoogleEmailVerificationNotifier implements EmailVerificationNotifier {

    private final JavaMailSender javaMailSender;

    public GoogleEmailVerificationNotifier(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    @Override
    public void sendVerification(String email, String rawToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("이메일 인증을 완료해주세요");
            message.setText("인증 토큰: " + rawToken);
            javaMailSender.send(message);

            log.info("이메일 발송 성공 대상: {}", email);
        } catch (MailException e) {
            log.error("이메일 발송 실패 대상: {}", email, e);
        }
    }
}