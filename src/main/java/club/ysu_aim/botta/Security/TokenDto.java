package club.ysu_aim.botta.Security;

import lombok.*;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class TokenDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;

}