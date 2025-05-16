package org.jupytereverywhere.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jupytereverywhere.dto.JupyterNotebookDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JupyterNotebookRequest {
  private String password;
  private JupyterNotebookDTO notebook;
}
