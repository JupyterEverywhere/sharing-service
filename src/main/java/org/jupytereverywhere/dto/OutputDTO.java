package org.jupytereverywhere.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OutputDTO {

  @JsonProperty("output_type")
  private String outputType;

  @JsonProperty("text")
  private List<String> text;

  @JsonProperty("data")
  private Map<String, Object> data;

  @JsonProperty("metadata")
  private Map<String, Object> metadata;

  @JsonProperty("name")
  private String name;

  @JsonProperty("ename")
  private String ename;

  @JsonProperty("evalue")
  private String evalue;

  @JsonProperty("traceback")
  private List<String> traceback;
}
