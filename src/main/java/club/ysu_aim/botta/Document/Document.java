package club.ysu_aim.botta.Document;

import club.ysu_aim.botta.Notebook.Notebook;
import club.ysu_aim.botta.Source.Source;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;
    
    @Column(columnDefinition = "TEXT")
    private String chunk;
    
    // PostgreSQL pgvector 연동을 고려해 데이터 타입을 vector로 명시하고 float[] 매핑
    @Column(columnDefinition = "vector")
    private float[] embeddings;
    
    // SourceMap 테이블을 엔티티 없이 @JoinTable을 활용한 다대다(ManyToMany) 관계로 매핑
    @ManyToMany
    @JoinTable(
            name = "source_map",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id")
    )
    private List<Source> sources = new ArrayList<>();
}
