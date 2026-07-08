package club.ysu_aim.botta.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users") // PostgreSQL에서 user는 예약어이므로 테이블명을 users로 지정
@Getter @Setter
@NoArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "password")
    private String password;
}
