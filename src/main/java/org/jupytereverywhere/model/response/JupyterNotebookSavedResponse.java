package org.jupytereverywhere.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JupyterNotebookSavedResponse implements JupyterNotebookResponse {

  private String message;
  private JupyterNotebookSaved notebook;
}
