package club.ysu_aim.botta.User;

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
@RestController
@RequestMapping
@RequiredArgsConstructor // 아래 final변수 두개 생성자 자동주입
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final RedisService redisService;

@Value("${jwt.refresh-expiration-time:1209600000}")
private long refreshTokenExpirationTime;



    // 회원가입 컨트롤러
    @PostMapping("/members")
    public ResponseEntity<?> join   (@RequestBody UserRequest request) {
        //중복검사
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 사용 중인 이메일입니다.");
        }
        //Entity 변환
        User newUser = request.toEntity();
        try {
            userRepository.save(newUser);
            UserResponse response = new UserResponse(null,"회원가입이 완료되었습니다.", null);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // DB 제약조건 위반 등 예외 발생 시 오류뱉음
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("회원가입 중 오류가 발생했습니다.");
        }
    }

    // 로그인 컨트롤러
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request, HttpServletResponse servletResponse) {

        // LoginId를 통해 DB에 등록된 유저인지 확인
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    if (user.getPassword().equals(request.getPassword())) {

                        // 로그인 성공 시 토큰 생성
                        String token = jwtTokenProvider.generateToken(user.getEmail());
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


                        UserResponse response = new UserResponse(token,"로그인 성공", user.getEmail());
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