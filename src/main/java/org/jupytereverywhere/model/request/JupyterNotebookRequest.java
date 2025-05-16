package org.jupytereverywhere.model.request;

import org.jupytereverywhere.dto.JupyterNotebookDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookRequest {
  private String password;
  private JupyterNotebookDTO notebook;
}
