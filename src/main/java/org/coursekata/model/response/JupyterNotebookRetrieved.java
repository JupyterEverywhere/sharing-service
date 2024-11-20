package org.coursekata.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.coursekata.dto.JupyterNotebookDTO;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JupyterNotebookRetrieved implements JupyterNotebookResponse {

    private UUID id;

    @JsonProperty("domain_id")
    private String domain;

    @JsonProperty("readable_id")
    private String readableId;

    @JsonProperty("content")
    private JupyterNotebookDTO notebookDTO;

}
