package org.jupytereverywhere.model.request;

import org.jupytereverywhere.dto.JupyterNotebookDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookRequest {
  @ToString.Exclude private String password;

  @NotNull(message = "Notebook field is required and cannot be null")
  @Valid
  private JupyterNotebookDTO notebook;
}
