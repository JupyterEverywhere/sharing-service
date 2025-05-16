package org.jupytereverywhere.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookDTO {
  private Integer nbformat;

  @JsonProperty("nbformat_minor")
  private Integer nbformatMinor;

  private MetadataDTO metadata;

  private List<Map<String, Object>> cells;
}

