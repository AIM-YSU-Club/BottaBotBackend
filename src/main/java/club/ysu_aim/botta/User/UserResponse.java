package club.ysu_aim.botta.User;

import lombok.Getter;
import lombok.ToString;

@ToString //로그찍기용
@Getter
public class UserResponse {
    private String token; //로그인토큰
    private String message; //확인 메세지
    private String loginId; //사용자가 로그인한 id

    public UserResponse(String token, String message, String loginId) {
        this.token = token;
        this.message = message;
        this.loginId = loginId;
    }


}