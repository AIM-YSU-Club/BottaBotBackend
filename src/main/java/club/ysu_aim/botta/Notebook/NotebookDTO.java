package club.ysu_aim.botta.Notebook;

import club.ysu_aim.botta.Notebook.Notebook;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import java.time.Instant;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NotebookDTO {

    private UUID notebookId;
    private String title;
    private String description;
    private Instant created_at;

    public Notebook toEntity() {
        return Notebook.builder()
                .notebookId(this.notebookId)
                .title(this.title)
                .description(this.description)
                .createdAt(this.created_at)
                .build();
    }

}
