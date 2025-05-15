package org.jupytereverywhere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class NotebookDTO {

  @JsonProperty("nbformat")
  private int nbformat;

  @JsonProperty("nbformat_minor")
  private int nbformatMinor;

  @JsonProperty("metadata")
  private MetadataDTO metadata;

  @JsonProperty("cells")
  private List<CellDTO> cells;
}
