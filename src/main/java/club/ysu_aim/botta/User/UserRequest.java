package club.ysu_aim.botta.User;


import club.ysu_aim.botta.User.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserRequest {

    private String refreshToken;
    private String email;
    private Long studentId;
//    private String loginId;
    private String password;
    private String name;
    private String nickname;
    //이미지 저장용 변수
//    private MultipartFile images;

    public User toEntity() {
        return User.builder()
                .email(this.email)
                .classNumber(this.studentId)
//                .loginId(this.loginId)
                .password(this.password) // 암호화된 비밀번호를 넣어야 함
                .name(this.name)
                .nickname(this.nickname)
                .build();
    }

}