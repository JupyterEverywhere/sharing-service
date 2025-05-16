package org.jupytereverywhere.model.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JupyterNotebookSaved {

    private UUID id;

    @JsonProperty("domain_id")
    private String domain;

    @JsonProperty("readable_id")
    private String readableId;

}
