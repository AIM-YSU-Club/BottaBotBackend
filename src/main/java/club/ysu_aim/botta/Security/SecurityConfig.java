
package club.ysu_aim.botta.Security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** 로그인 토근 인증로직 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // throw : 메서드 실행 중 에러가 발생했을 때, 해당 예외를 직접 처리하지 않고 메서드를 호출한 곳으로 던지는(전가하는) 자바의 문법
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");


                })
            )
            .authorizeHttpRequests(auth -> auth /* 어떤 주소로 들어오는 요청을 허용하거나 막을지 정함 */
                    // OPTIONS 메소드는 로그인 없이 통과시킴.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/", "/*.html", "/assets/**", "/favicon.ico", "/static/**", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.onnx", "/*.txt").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**","/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                    .requestMatchers(
                            // 로그인은 비로그인 상태에서 호출되고 있으므로 통과시킴.
                            "/api/v1/auth/login",
                            // 토큰 갱신은 JWT가 만료된 시점에서 호출되므로 통과시킴.
                            "/api/v1/auth/refresh"
                            // 로그아웃은 현재 별도 인증 절차를 거치지 않고 있으므로 통과시킴.
                            "/api/v1/auth/logout"
                    ).permitAll()
                    // 회원가입 API는 비로그인 상태에서 호출되므로 JWT 없어도 통과
                    .requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                    // 이메일 인증 API는 비로그인 상태에서 호출되므로 JWT 없어도 통과
                    .requestMatchers("/api/v1/members/email-verification", "/api/v1/members/email-verification/**").permitAll()
                .anyRequest().authenticated() /* 그 외 모든 페이지 요청은 인증 필요 */
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); /* 아이디/비번을 치기 전에 이미 토큰을 들고 온 사람인지 먼저 확인해서, 인증이 됐다면 바로 통과시켜주기 위한 코드 */

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000", "http://localhost:5173", "http://localhost:8080", "https://*.trycloudflare.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 수용
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
