package org.jupytereverywhere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KernelspecDTO {

  @JsonProperty("display_name")
  private String displayName;

  @JsonProperty("language")
  private String language;

  @JsonProperty("name")
  private String name;
}
