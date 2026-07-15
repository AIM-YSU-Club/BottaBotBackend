package club.ysu_aim.botta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 서버 접속 확인 로그 메소드
 */
@Component
public class RedisConnectionChecker implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(RedisConnectionChecker.class);
    private final StringRedisTemplate redisTemplate;
    
    public RedisConnectionChecker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void run(String ... args) {
        try {
            // Redis 서버에 PING 명령을 날려 연결 상태를 직접 확인
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            String pong = connection.ping();
            
            log.info("🍏 [Redis] 연결 검증 성공! 서버 응답: {}", pong);
        } catch (Exception e) {
            log.error("🍎 [Redis] 연결 검증 실패! Redis 서버 상태나 설정을 확인하세요.", e);
            
            // 만약 Redis 연결 실패 시 애플리케이션을 강제 종료하고 싶다면 아래 주석 해제
            // throw new IllegalStateException("Redis connection failed on startup", e);
        }
    }
}
