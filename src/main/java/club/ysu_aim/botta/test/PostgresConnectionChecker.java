package club.ysu_aim.botta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Postgres 연결 확인 로그 생성 메소드
 */
@Component
public class PostgresConnectionChecker implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PostgresConnectionChecker.class);
    private final JdbcTemplate jdbcTemplate;
    
    public PostgresConnectionChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void run(String ... args) {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                log.info("✅[Postgres] 연결 검증 성공! 데이터베이스가 준비되었습니다.");
            }
        } catch (Exception e) {
            log.error("❌[Postgres] 연결 검증 실패! DB 상태나 .env 설정을 확인하세요.", e);
        }
    }
}
