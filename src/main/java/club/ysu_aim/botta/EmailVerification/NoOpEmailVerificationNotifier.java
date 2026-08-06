package club.ysu_aim.botta.EmailVerification;

import org.springframework.stereotype.Component;

/** 메일 발송을 도입하기 전까지 사용하는 무동작 구현체. 토큰을 로그에도 남기지 않는다. */
@Component
public class NoOpEmailVerificationNotifier implements EmailVerificationNotifier {
    /**
     * 메일 발송 연동 전까지 호출 계약만 유지하고 외부 작업은 수행하지 않는다.
     *
     * @param email 인증 대상 이메일
     * @param rawToken 이메일 링크에 포함할 원문 토큰
     */
    @Override
    public void sendVerification(String email, String rawToken) {
    }
}
