package org.jupytereverywhere.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookDTO {
  @NotNull(message = "nbformat is required")
  private Integer nbformat;

  @JsonProperty("nbformat_minor")
  @NotNull(message = "nbformat_minor is required")
  private Integer nbformatMinor;

  @NotNull(message = "Metadata is required")
  @Valid
  private MetadataDTO metadata;

  @NotNull(message = "Cells array is required")
  private List<Map<String, Object>> cells;
}

