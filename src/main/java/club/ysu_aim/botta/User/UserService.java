package club.ysu_aim.botta.User;

import club.ysu_aim.botta.EmailVerification.EmailVerficationService;
import club.ysu_aim.botta.Security.JwtTokenProvider;
import club.ysu_aim.botta.Security.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerficationService emailVerificationService;

    /**
     * 회원 정보를 저장하고 동일 트랜잭션에서 최초 이메일 인증 토큰을 발급한다.
     * 비밀번호는 저장 전에 BCrypt로 단방향 암호화한다.
     *
     * @param request 회원가입 요청 정보
     * @return 영속화된 회원
     */
    @Transactional
    public User register(UserRequest request) {
        User user = request.toEntity();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        User savedUser = userRepository.save(user);
        emailVerificationService.issueForNewUser(savedUser);
        return savedUser;
    }

    public TokenDto refreshAccessToken(String refreshToken) {

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
        String email = tokenProvider.getEmailFromToken(refreshToken);
        String storedRefreshToken = redisService.getRefreshToken(email);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("토큰 정보가 일치하지 않습니다. 다시 로그인하세요.");
        }

        return tokenProvider.createToken(email);
    }
    @Transactional
    public void logout(String refreshToken) {

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);

        redisService.deleteValues(email);
    }
}
