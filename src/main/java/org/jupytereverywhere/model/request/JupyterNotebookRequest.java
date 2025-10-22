package org.jupytereverywhere.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jupytereverywhere.dto.JupyterNotebookDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookRequest {
  private String password;

  @NotNull(message = "Notebook field is required and cannot be null")
  @Valid
  private JupyterNotebookDTO notebook;
}
