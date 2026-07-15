package club.ysu_aim.botta.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User API", description = "지랄지랄지랄지랄")
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Operation(summary = "테스트", description = "테에스으트으")
    @PostMapping("/tesssst")
    public TestResponse testblyat(@RequestBody TestRequest request) {
        return new TestResponse(1L, request.getName(), "SUCCESS");
    }
}

@Getter @Setter
class TestRequest {
    @Schema(description = "cyka", example = "blyat")
    private String name;
}

@Getter @Setter
class TestResponse {
    @Schema(description = "blyat", example = "1")
    private Long id;
    
    @Schema(description = "bllyat", example = "2")
    private String name;
    
    @Schema(description = "blllyat", example = "3")
    private String status;
    
    public TestResponse(Long id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }
}
