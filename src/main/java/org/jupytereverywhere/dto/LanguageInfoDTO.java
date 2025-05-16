package org.jupytereverywhere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LanguageInfoDTO {

  @JsonProperty("codemirror_mode")
  private CodemirrorModeDTO codemirrorMode;

  @JsonProperty("file_extension")
  private String fileExtension;

  @JsonProperty("mimetype")
  private String mimetype;

  @JsonProperty("name")
  private String name;

  @JsonProperty("nbconvert_exporter")
  private String nbconvertExporter;

  @JsonProperty("version")
  private String version;
}
