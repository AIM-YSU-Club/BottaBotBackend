package club.ysu_aim.botta.User;

import club.ysu_aim.botta.Security.JwtTokenProvider;
import club.ysu_aim.botta.Security.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;

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