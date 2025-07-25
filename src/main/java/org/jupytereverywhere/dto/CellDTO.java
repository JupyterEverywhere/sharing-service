package org.jupytereverywhere.dto;


import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CellDTO {

  @JsonProperty("cell_type")
  private String cellType;

  @JsonProperty("metadata")
  private Map<String, Object> metadata;

  @JsonProperty("source")
  private List<String> source;

  @JsonProperty("execution_count")
  private Integer executionCount;

  @JsonProperty("outputs")
  private List<OutputDTO> outputs;
}
