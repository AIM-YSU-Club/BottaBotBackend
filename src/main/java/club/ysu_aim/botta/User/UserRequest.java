package club.ysu_aim.botta.User;


import club.ysu_aim.botta.User.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "회원 및 인증 요청 정보")
public class UserRequest {

    @Schema(description = "Refresh Token")
    private String refreshToken;
    @Schema(description = "회원 이메일", example = "member@example.com")
    private String email;
    @Schema(description = "학번", example = "20260001")
    private String studentId;
//    private String loginId;
    @Schema(description = "비밀번호", example = "password123!")
    private String password;
    @Schema(description = "회원 이름", example = "김보타")
    private String name;
    @Schema(description = "회원 닉네임", example = "보타")
    private String nickname;
    //이미지 저장용 변수
//    private MultipartFile images;

    /**
     * 회원 요청 값을 신규 회원 엔티티로 변환한다.
     * 비밀번호 암호화와 이메일 정규화는 서비스 계층에서 수행한다.
     *
     * @return 아직 영속화되지 않은 회원 엔티티
     */
    public User toEntity() {
        return User.builder()
                .email(this.email)
                .studentId(this.studentId)
//                .loginId(this.loginId)
                .name(this.name)
                .nickname(this.nickname)
                .build();
    }

}
