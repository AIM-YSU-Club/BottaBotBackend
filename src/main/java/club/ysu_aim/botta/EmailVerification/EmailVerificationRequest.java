package club.ysu_aim.botta.EmailVerification;

import io.swagger.v3.oas.annotations.media.Schema;

/** 이메일 인증 API에서 사용하는 요청 본문을 모아둔다. */
public final class EmailVerificationRequest {
    private EmailVerificationRequest() {
    }

    @Schema(description = "이메일 인증 메일 발송 또는 재발송 요청")
    public record Send(
            @Schema(description = "인증 대상 이메일", example = "member@example.com",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String email,
            @Schema(description = "이메일 인증 및 필수 약관 동의 여부", example = "true",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Boolean agreeTerms) {
    }
}
