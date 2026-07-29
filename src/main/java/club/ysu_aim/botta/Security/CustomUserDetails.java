package club.ysu_aim.botta.Security;

import club.ysu_aim.botta.User.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;      // PK (식별자)
    private final String email;     // 이메일 (로그인 ID로 사용)
    private final String password;  // 암호화된 비밀번호


    // 생성자 (User 엔티티를 받아서 빌드하는 방식으로 만들면 편해)
    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getHashedPass(); // 엔티티의 hashed_pass 필드 매핑
    }

    // 💡 우리 비즈니스 로직에 필요한 Getter 추가
    public UUID getUserId() {
        return this.userId;
    }

    // --- UserDetails 필수 구현 메서드들 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 유저의 권한 역할을 반환 (기본값으로 일반 유저 권한 부여 예시)
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email; // 우리는 이메일로 로그인하니까 email을 리턴!
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}