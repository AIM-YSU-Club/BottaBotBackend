package club.ysu_aim.botta.EmailVerification;

import org.springframework.http.HttpStatus;

public class EmailVerificationException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public EmailVerificationException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
