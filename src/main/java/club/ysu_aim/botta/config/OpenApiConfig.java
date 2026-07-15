package club.ysu_aim.botta.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger springdoc-ui 구성 파일.
 * 기존의 SwaggerConfig.java 파일이랑 중복으로 이 파일만 사용
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Ysu Aim Botta API")
                .version("v0.0.1-PRE_ALPHA")
                .description("Ysu Aim Botta API");
        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}
