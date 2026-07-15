package club.ysu_aim.botta.File;

import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 파일 — Source에 연결된 물리 파일 메타데이터 및 마크다운 원문.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class File {

    /** 파일 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    /** 소속 소스 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    /** 파일명 */
    @Column(name = "file_name", columnDefinition = "TEXT")
    private String fileName;

    /** 저장 경로 */
    @Column(columnDefinition = "TEXT")
    private String path;

    /** 마크다운으로 변환된 원문 */
    @Column(columnDefinition = "TEXT")
    private String markdown;
}
