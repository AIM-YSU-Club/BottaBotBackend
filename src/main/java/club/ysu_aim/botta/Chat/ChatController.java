package club.ysu_aim.botta.Chat;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개별 대화(Chat) 관련 API 컨트롤러.
 * <p>
 * 대화 세션 및 메시지 스트리밍 엔드포인트는
 * {@link club.ysu_aim.botta.ChatSession.ChatSessionController}에서 통합 관리됩니다.
 * </p>
 */
@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {
}
