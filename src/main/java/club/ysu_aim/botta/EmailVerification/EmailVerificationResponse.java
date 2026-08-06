package club.ysu_aim.botta.EmailVerification;

import io.swagger.v3.oas.annotations.media.Schema;

/** 이메일 인증 요청 또는 확인 결과다. */
@Schema(description = "이메일 인증 처리 결과")
public record EmailVerificationResponse(
        @Schema(description = "인증 메일 발송 요청 접수 여부", example = "true")
        Boolean accepted,
        @Schema(description = "이메일 인증 완료 여부", example = "true")
        Boolean verified,
        @Schema(description = "처리 결과 설명", example = "이메일 인증이 완료되었습니다.")
        String message) {

    /** 메일 발송 요청이 접수된 응답을 만든다. */
    public static EmailVerificationResponse acceptedResult() {
        return new EmailVerificationResponse(true, null, "이메일 인증 요청이 접수되었습니다.");
    }

    /** 토큰 검증과 이메일 인증이 완료된 응답을 만든다. */
    public static EmailVerificationResponse verifiedResult() {
        return new EmailVerificationResponse(null, true, "이메일 인증이 완료되었습니다.");
    }
}
