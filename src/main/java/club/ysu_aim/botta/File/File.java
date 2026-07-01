package club.ysu_aim.botta.File;

import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;
    
    @Column(columnDefinition = "TEXT")
    private String fileName;
    
    @Column(columnDefinition = "TEXT")
    private String path;
    
    @Column(columnDefinition = "TEXT")
    private String markdown;
}
