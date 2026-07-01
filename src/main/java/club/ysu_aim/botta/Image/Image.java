package club.ysu_aim.botta.Image;

import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Image {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;
    
    @Column(columnDefinition = "TEXT")
    private String imageName;
    
    @Column(columnDefinition = "TEXT")
    private String vlmCaption;
}
