package club.ysu_aim.botta.Webpage;

/*
 * [보류] 현재 ERD에 Webpage 테이블이 없어 엔티티를 비활성화함.
 * Hibernate ddl-auto에 테이블이 생성되지 않도록 @Entity 포함 전체를 주석 처리.
 * Source 하위 웹페이지 메타데이터로 추후 재도입 가능.
 *
 * import club.ysu_aim.botta.Source.Source;
 * import jakarta.persistence.*;
 * import lombok.Getter;
 * import lombok.NoArgsConstructor;
 * import lombok.Setter;
 *
 * import java.util.UUID;
 *
 * @Entity
 * @Getter
 * @Setter
 * @NoArgsConstructor
 * public class Webpage {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.UUID)
 *     @Column(name = "webpage_id", nullable = false, updatable = false)
 *     private UUID webpageId;
 *
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     @JoinColumn(name = "source_id", nullable = false)
 *     private Source source;
 *
 *     @Column(name = "page_name", columnDefinition = "TEXT")
 *     private String pageName;
 *
 *     @Column(columnDefinition = "TEXT")
 *     private String url;
 *
 *     @Column(columnDefinition = "TEXT")
 *     private String markdown;
 * }
 */
