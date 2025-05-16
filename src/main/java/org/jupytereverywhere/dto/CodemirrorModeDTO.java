package org.jupytereverywhere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodemirrorModeDTO {

  @JsonProperty("name")
  private String name;

  @JsonProperty("version")
  private Integer version;
}
