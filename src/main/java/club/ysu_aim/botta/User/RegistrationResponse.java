package club.ysu_aim.botta.User;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** 회원가입 완료 후 생성된 회원과 인증 메일 발송 상태를 반환한다. */
@Schema(description = "회원가입 결과")
public record RegistrationResponse(
        @Schema(description = "생성된 회원 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID memberId,
        @Schema(description = "인증 메일의 실제 발송 여부. 메일 연동 전에는 false", example = "false")
        boolean verificationEmailSent) {
}
