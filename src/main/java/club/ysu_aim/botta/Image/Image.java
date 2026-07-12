package club.ysu_aim.botta.Image;

/*
 * [보류] 현재 ERD에 Image 테이블이 없어 엔티티를 비활성화함.
 * Hibernate ddl-auto에 테이블이 생성되지 않도록 @Entity 포함 전체를 주석 처리.
 * Source 하위 이미지 메타데이터로 추후 재도입 가능.
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
 * public class Image {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.UUID)
 *     @Column(name = "image_id", nullable = false, updatable = false)
 *     private UUID imageId;
 *
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     @JoinColumn(name = "source_id", nullable = false)
 *     private Source source;
 *
 *     @Column(name = "image_name", columnDefinition = "TEXT")
 *     private String imageName;
 *
 *     @Column(name = "vlm_caption", columnDefinition = "TEXT")
 *     private String vlmCaption;
 * }
 */
