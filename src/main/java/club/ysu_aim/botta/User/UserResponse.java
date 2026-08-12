package club.ysu_aim.botta.User;

import lombok.Getter;
import lombok.ToString;

@ToString //로그찍기용
@Getter
public class UserResponse {
    private String accessToken; //로그인 Access Token
    private String message; //확인 메세지
    private String loginId; //사용자가 로그인한 id

    public UserResponse(String accessToken, String message, String loginId) {
        this.accessToken = accessToken;
        this.message = message;
        this.loginId = loginId;
    }


}
