package club.ysu_aim.botta.User;

import club.ysu_aim.botta.common.ApiEnvelope;
import club.ysu_aim.botta.User.UserService;
import club.ysu_aim.botta.User.UserResponse;
import club.ysu_aim.botta.User.UserRequest;
import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserRepository;
import club.ysu_aim.botta.Security.TokenDto;
import club.ysu_aim.botta.Security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
//import com.example.back.Service.Redis.RedisService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j //로그찍기
@Tag(name = "회원/인증", description = "회원가입, 로그인 및 JWT 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor // 아래 final변수 두개 생성자 자동주입
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

@Value("${jwt.refresh-expiration-time:1209600000}")
private long refreshTokenExpirationTime;



    /**
     * 자체 회원가입을 처리하고 이메일 인증에 사용할 토큰을 함께 발급한다.
     * 실제 인증 메일 발송 구현은 제외되어 응답의 verificationEmailSent는 false다.
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 회원 식별자와 인증 메일 발송 여부
     */
    @Operation(
            summary = "자체 회원가입",
            description = "회원 정보를 저장하고 이메일 인증 토큰을 발급합니다. 생성된 memberId와 인증 메일 발송 여부를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "필수 회원 정보 누락",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
            @ApiResponse(responseCode = "500", description = "회원가입 처리 실패",
                    content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    @PostMapping("/members")
    public ResponseEntity<ApiEnvelope<RegistrationResponse>> join(@RequestBody UserRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiEnvelope.failure("INVALID_REQUEST", "이메일과 비밀번호는 필수입니다."));
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        request.setEmail(normalizedEmail);
        //중복검사
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiEnvelope.failure("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."));
        }
        try {
            User registeredUser = userService.register(request);
            return ResponseEntity.ok(ApiEnvelope.success(
                    new RegistrationResponse(registeredUser.getUserId(), false)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiEnvelope.failure("INTERNAL_ERROR", "회원가입 중 오류가 발생했습니다."));
        }
    }

    /**
     * 이메일과 비밀번호를 검증하고 이메일 인증 회원에게만 JWT를 발급한다.
     *
     * @param request 로그인 이메일과 비밀번호
     * @param servletResponse Refresh Token 쿠키를 기록할 HTTP 응답
     * @return Access Token 및 로그인 결과
     */
    @Operation(
            summary = "이메일 로그인",
            description = "이메일과 비밀번호를 검증하고 이메일 인증이 완료된 회원에게 Access/Refresh Token을 발급합니다.")
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request, HttpServletResponse servletResponse) {

        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("아이디 혹은 비밀번호가 틀렸습니다.");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // LoginId를 통해 DB에 등록된 유저인지 확인
        return userRepository.findByEmail(normalizedEmail)
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getHashedPass())) {

                        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body("EMAIL_NOT_VERIFIED: 이메일 인증이 필요합니다.");
                        }

                        // 로그인 성공 시 토큰 생성
                        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
                        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
                        redisService.setRefreshToken(user.getEmail(), refreshToken, refreshTokenExpirationTime);

                        // 보안 옵션 설정
                        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

                        refreshCookie.setHttpOnly(true);   // 자바스크립트(XSS 공격)로 접근 불가 설정
//                        refreshCookie.setSecure(true);     // HTTPS 환경에서만 쿠키를 전송하도록 설정
                        refreshCookie.setPath("/");        // 모든 경로에서 이 쿠키가 서버로 전송되도록 설정
                        refreshCookie.setMaxAge((int) (refreshTokenExpirationTime / 1000)); // 쿠키 만료 시간 (초 단위)

                        // 응답 헤더에 쿠키 추가
                        servletResponse.addCookie(refreshCookie); //브라우저에게 보내는 HTTP 응답 메시지의 헤더에 Set-Cookie라는 특수한 한 줄을 추가


                        UserResponse response = new UserResponse(accessToken, "로그인 성공", user.getEmail());
                        log.info("로그인 응답 데이터: {}", response.toString());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 혹은 비밀번호가 틀렸습니다.");
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 사용자입니다."));
    }
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                    HttpServletResponse servletResponse) {
        if (refreshToken != null) {
            try {
                // userService에서 로그인 유저의 id를 찾아 해당 id 주인의 redis에서 리프레시 토큰을 제거
                userService.logout(refreshToken);
            } catch (Exception e) {
                log.error("로그아웃 중 Redis 토큰 삭제 실패: {}", e.getMessage());
            }
        }

        Cookie deleteCookie = new Cookie("refreshToken",null);
        deleteCookie.setHttpOnly(true);
        deleteCookie.setSecure(true);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        servletResponse.addCookie(deleteCookie);

        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshAccessToken(
            // 브라우저가 자동으로 실어 보낸 쿠키 중 "refreshToken"이라는 이름을 가진 값을 쏙 빼옴
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        // 만약 쿠키에 리프레시 토큰이 없다면 (로그아웃했거나 쿠키가 만료된 경우)
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("리프레시 토큰이 존재하지 않습니다. 다시 로그인해주세요.");
        }

        // 서비스 계층으로 쿠키에서 꺼낸 토큰 문자열을 그대로 넘겨줌
        TokenDto tokenSet = userService.refreshAccessToken(refreshToken);
        String AccessToken = tokenSet.getAccessToken();

        // 새로 발급된 액세스 토큰 포함을 반환
        return ResponseEntity.ok(AccessToken);
    }
//
//    @PostMapping("/auth/password-reset/request")
//    @PostMapping("/auth/find-id")
//    @PatchMapping("/auth/password-reset/confirm")
//    @GetMapping("/members/email-verification?token=")
//    @GetMapping("/members/me")
//    @PatchMapping("/members/me")
//    @DeleteMapping("/members/me")

}
