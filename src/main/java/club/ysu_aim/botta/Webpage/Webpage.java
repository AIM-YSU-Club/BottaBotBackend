package club.ysu_aim.botta.Webpage;

import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Webpage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "webpage_id")
    private Long webpageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;
    
    @Column(columnDefinition = "TEXT")
    private String pageName;
    
    @Column(columnDefinition = "TEXT")
    private String url;
    
    @Column(columnDefinition = "TEXT")
    private String markdown;
}
