package club.ysu_aim.botta.common;

import io.swagger.v3.oas.annotations.media.Schema;

/** API 요청이 실패했을 때 클라이언트에 전달하는 오류 정보다. */
@Schema(description = "API 오류 정보")
public record ApiError(
        @Schema(description = "클라이언트가 분기 처리할 오류 코드", example = "INVALID_REQUEST")
        String code,
        @Schema(description = "사용자에게 표시할 수 있는 오류 설명", example = "요청값이 올바르지 않습니다.")
        String message) {
}
