package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.common.ApiEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API 명세서의 MEM02 이메일 인증 요청과 확인 API를 제공한다. */
@Tag(name = "회원 이메일 인증", description = "인증 메일 발송 요청 및 인증 토큰 확인 API")
@RestController
@RequestMapping("/api/v1/members/email-verification")
@lombok.RequiredArgsConstructor
public class EmailVerficationController {
    private final EmailVerficationService verificationService;

    /**
     * 이메일 인증 메일 발송 또는 재발송 요청을 접수한다.
     * 실제 메일 전송은 추후 notifier 구현체가 추가되면 수행된다.
     */
    @Operation(
            summary = "이메일 인증 메일 발송 요청",
            description = "이메일과 약관 동의 여부를 확인한 후 인증 토큰을 새로 발급합니다. "
                    + "계정 존재 여부는 응답으로 노출하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "인증 메일 발송 요청 접수"),
            @ApiResponse(responseCode = "400", description = "필수 입력값 또는 약관 동의 누락",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "429", description = "재발송 횟수 또는 요청 간격 제한 초과",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    @PostMapping
    public ResponseEntity<ApiEnvelope<EmailVerificationResponse>> send(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "이메일 인증 발송 요청 정보")
            @RequestBody EmailVerificationRequest.Send request) {
        verificationService.requestVerification(request.email(), request.agreeTerms());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiEnvelope.success(EmailVerificationResponse.acceptedResult()));
    }

    /** 이메일 링크의 토큰을 검증하고 대상 회원을 인증 완료 상태로 변경한다. */
    @Operation(
            summary = "이메일 인증 토큰 확인",
            description = "이메일의 인증 링크에 포함된 일회성 토큰을 검증하고 회원의 이메일 인증을 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 완료"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용된 토큰",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "410", description = "만료된 토큰",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    @GetMapping
    public ApiEnvelope<EmailVerificationResponse> confirm(
            @Parameter(description = "이메일 인증 링크에 포함된 일회성 토큰", required = true,
                    example = "RkFjS2Q0bXJfV2hZbXJ1Y3RrQm5QZzV1aW9Y")
            @RequestParam String token) {
        verificationService.confirm(token);
        return ApiEnvelope.success(EmailVerificationResponse.verifiedResult());
    }
}
