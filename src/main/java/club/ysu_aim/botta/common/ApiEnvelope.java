package club.ysu_aim.botta.common;

import io.swagger.v3.oas.annotations.media.Schema;

/** API 명세서의 공통 성공/실패 응답 형식을 표현한다. */
@Schema(description = "BottaBot API 공통 응답")
public record ApiEnvelope<T>(
        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,
        @Schema(description = "성공 응답 데이터. 실패 시 null")
        T data,
        @Schema(description = "실패 정보. 성공 시 null")
        ApiError error) {

    /** 성공 결과를 공통 응답 형식으로 감싼다. */
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>(true, data, null);
    }

    /** 실패 결과를 공통 응답 형식으로 감싼다. */
    public static <T> ApiEnvelope<T> failure(String code, String message) {
        return new ApiEnvelope<>(false, null, new ApiError(code, message));
    }
}
