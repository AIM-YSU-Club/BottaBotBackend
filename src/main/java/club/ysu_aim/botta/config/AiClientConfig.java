package club.ysu_aim.botta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * BottaBotAI 호출용 RestClient 구성.
 * 기본 JDK HttpClient는 HTTP/2 업그레이드를 시도해 FastAPI(Starlette)가
 * multipart 폼을 못 읽는 경우가 있어 HTTP/1.1로 고정한다.
 */
@Configuration
public class AiClientConfig {

    /**
     * AI 서버 base-url을 사용하는 RestClient.
     *
     * @param baseUrl application.yaml의 {@code ai.server.base-url}
     * @return HTTP/1.1 RestClient
     */
    @Bean
    public RestClient aiRestClient(@Value("${ai.server.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(2));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
