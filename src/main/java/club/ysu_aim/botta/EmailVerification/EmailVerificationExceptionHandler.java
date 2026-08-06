package club.ysu_aim.botta.EmailVerification;

import club.ysu_aim.botta.common.ApiEnvelope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EmailVerificationExceptionHandler {
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handle(EmailVerificationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiEnvelope.failure(exception.getCode(), exception.getMessage()));
    }
}
