package club.ysu_aim.botta.EmailVerification;

/** 실제 메일 발송 구현체가 나중에 연결될 경계이다. */
public interface EmailVerificationNotifier {
    /**
     * 인증 대상에게 원문 토큰이 포함된 인증 안내를 전달한다.
     *
     * @param email 인증 대상 이메일
     * @param rawToken 인증 링크를 구성할 일회성 원문 토큰
     */
    void sendVerification(String email, String rawToken);
}
