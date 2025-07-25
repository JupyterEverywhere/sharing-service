package org.jupytereverywhere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetadataDTO {

  @JsonProperty("kernelspec")
  private KernelspecDTO kernelspec;

  @JsonProperty("language_info")
  private LanguageInfoDTO languageInfo;
}
