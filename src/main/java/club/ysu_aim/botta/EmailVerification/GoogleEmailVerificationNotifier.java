package club.ysu_aim.botta.EmailVerification;

import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void sendVerification(String email, String randomNum) {
        try {
            String htmlContent = String.format("""
                    <!DOCTYPE html>
                            <html lang="ko">
                            <head>
                                <meta charset="UTF-8">
                                <title>이메일 인증</title>
                            </head>
                            <body style="margin: 0; padding: 0; background-color: #f1f5f9;">
                                <div style="padding: 50px 20px; font-family: sans-serif;">
                                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 12px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                                        <h2 style="color: #1e293b; font-size: 24px; margin-top: 0; margin-bottom: 20px;">이메일 인증번호 안내</h2>
                                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 30px;">
                                            BottaBot 서비스 가입을 환영합니다.<br>
                                            원활한 서비스 이용을 위해 아래 인증번호를 가입 화면에 입력해 주세요.
                                        </p>
                    
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; margin-bottom: 30px;">
                                            <span style="font-size: 36px; font-weight: bold; color: #4A90E2; letter-spacing: 12px; margin-left: 12px;">%s</span>
                                        </div>
                    
                                        <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0;">
                                            본 인증번호의 유효시간은 5분입니다.<br>
                                            안전을 위해 타인에게 절대 유출하거나 공유하지 마세요.to
                                        </p>
                                    </div>
                                </div>
                            </body>
                            </html>
                """, randomNum);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("이메일 인증을 완료해주세요");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);

            log.info("이메일 발송 성공 대상: {}", email);
        } catch (Exception e) {
            log.error("이메일 발송 실패 대상: {}", email, e);
        }
    }
}