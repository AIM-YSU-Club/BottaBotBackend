package club.ysu_aim.botta.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService; //스프링 시큐리티 자체 인터페이스


    @Lazy
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getJwtFromRequest(request);
        log.info("JwtAuthenticationFilter - Request URI: {}, Token: {}", request.getRequestURI(), token != null ? "Present" : "Absent");

        // 토큰이 있고 유효한지 확인
        if (StringUtils.hasText(token)) {
            try {
                if (tokenProvider.validateToken(token)) {
                    // 토큰에서 로그인 ID 추출
                    String email = tokenProvider.getEmailFromToken(token);
                    log.info("JwtAuthenticationFilter - Valid token for user: {}", email);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, /*아이디, 이름, 이메일 등 유저의 상세 정보가 담긴 객체*/
                                null,
                                userDetails.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else if (isPublicAuthRequest(request)) {
                    // 로그인/회원가입 등은 Authorization에 만료 토큰이 남아 있어도 통과
                    log.warn("JwtAuthenticationFilter - Ignoring invalid token on public auth URI: {}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                } else {
                    log.warn("JwtAuthenticationFilter - Invalid token provided for URI: {}", request.getRequestURI());

                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 에러
                    response.getWriter().write("{\"code\": \"TOKEN_EXPIRED\", \"message\": \"액세스 토큰이 만료되었습니다.\"}");
                    return;
                }
            } catch (Exception e) {
                log.error("JwtAuthenticationFilter - Error processing token: {}", e.getMessage());
                // 여기서 에러를 던지지 않고 그냥 filterChain을 타게 함으로써 permitAll 경로가 작동하도록 유도
            }
        }

        filterChain.doFilter(request, response); //"나는 내 할 일을 다 했으니, 다음 필터에게 넘겨라"는 뜻
    }

    /** 비로그인으로 호출되는 인증/회원 진입 API */
    private boolean isPublicAuthRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri == null) {
            return false;
        }
        if ("POST".equalsIgnoreCase(method) && (
                "/api/v1/auth/login".equals(uri)
                        || "/api/v1/auth/refresh".equals(uri)
                        || "/api/v1/auth/logout".equals(uri)
                        || "/api/v1/members".equals(uri))) {
            return true;
        }
        return uri.startsWith("/api/v1/members/email-verification");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}