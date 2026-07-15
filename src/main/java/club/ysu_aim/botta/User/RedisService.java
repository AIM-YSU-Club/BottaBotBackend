package club.ysu_aim.botta.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor // 변수 주입을 위한 생성자 자동 선언 (생성자 주입!)
public class RedisService {

    // 스프링이 제공하는 가장 편리한 Redis 조작 부품입니다. (Key, Value 모두 문자열로 처리)
    private final StringRedisTemplate redisTemplate;

    /**
     * [1. 토큰 저장 및 갱신]
     * 로그인 성공 시 유저의 LoginId를 Key로, 리프레시 토큰을 Value로 저장합니다.
     * @param email 유저 아이디 (Key)
     * @param refreshToken 리프레시 토큰 문자열 (Value)
     * @param timeout 만료 시간 (밀리초 단위)
     */
    public void setRefreshToken(String email, String refreshToken, long timeout) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();

        // 대입 직후 만료시간이 지나면 자동으로 Redis 금고에서 파기되도록 설정합니다.
        values.set(
                "RT:" + email, // Key 앞에 'RT:' 같은 접두사(Prefix)를 붙여주면 나중에 관리하기 좋습니다.
                refreshToken,
                timeout,
                TimeUnit.MILLISECONDS // 밀리초 단위로 수명 지정
        );
        log.info("Redis에 리프레시 토큰 저장 완료 -> Key: RT:{}", email);
    }

    /**
     * [2. 토큰 조회]
     * 토큰 재발급 요청이 왔을 때, Redis 금고에 보관된 진짜 토큰을 꺼내옵니다.
     * @param email 유저 아이디
     * @return 저장된 리프레시 토큰 문자열
     */
    public String getRefreshToken(String email) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get("RT:" + email);
    }

    /**
     * [3. 토큰 삭제 - 로그아웃용]
     * 유저가 로그아웃하면 Redis 금고에서 토큰을 완전히 갈아버립니다.
     * @param email 유저 아이디
     */
    public void deleteValues(String email) {
        // 해당 키가 존재하면 삭제하고 true를, 없으면 false를 뱉습니다.
        Boolean isDeleted = redisTemplate.delete("RT:" + email);

        if (Boolean.TRUE.equals(isDeleted)) {
            log.info("Redis에서 리프레시 토큰 무효화 완료 -> Key: RT:{}", email);
        } else {
            log.warn("Redis 삭제 실패: 존재하지 않는 Key입니다 -> RT:{}", email);
        }
    }
}