//package club.ysu_aim.botta.ChatSession;
//
//import club.ysu_aim.botta.ChatSession.ChatSession;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.UUID;
//
//public class ChatSessionService {
//
//    @Transactional
//    public void deleteChatSession(UUID userId, UUID sessionId) {
//        ChatSession chatSession = validateAndGetChatSession(userId, sessionId);
//        ChatSessionRepository.delete(chatSession);
//    }
//}
