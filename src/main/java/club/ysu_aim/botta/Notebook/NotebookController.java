package club.ysu_aim.botta.Notebook;


import club.ysu_aim.botta.Security.CustomUserDetailsService;
import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.User.User;
import club.ysu_aim.botta.User.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class NotebookController {

    @PostMapping("/notebooks")
    public ResponseEntity<NotebookDTO> register (@RequestBody NotebookDTO request, HttpServletResponse servletResponse,@AuthenticationPrincipal CustomUserDetailsService userDetails) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 사용 중인 이메일입니다.");
        }
        //Entity 변환
        Notebook newUser = request.toEntity();
        try {
            userRepository.save(newUser);
            UserResponse response = new UserResponse(null,"회원가입이 완료되었습니다.", null);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // DB 제약조건 위반 등 예외 발생 시 오류뱉음
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("회원가입 중 오류가 발생했습니다.");
        }
    }
    @GetMapping("/notebooks")
    public ResponseEntity<?> check (@RequestBody NotebookDTO request, HttpServletResponse servletResponse) {

    }
    @GetMapping("/notebooks/{notebookId}")
    public ResponseEntity<?> detail_check (@DestinationVariable String notebookId, @RequestBody NotebookDTO request, HttpServletResponse servletResponse) {

    }
    @PatchMapping("/notebooks/{notebookId}")
    public ResponseEntity<?> modify (@DestinationVariable String notebookId, @RequestBody NotebookDTO request, HttpServletResponse servletResponse) {

    }
    @PatchMapping("/notebooks/{notebookId}")
    public ResponseEntity<?> delete (@DestinationVariable String notebookId, @RequestBody NotebookDTO request, HttpServletResponse servletResponse) {

    }
}
