package org.jupytereverywhere.service;

import java.util.Map;

import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.springframework.stereotype.Service;

@Service
public interface StorageService {
  String uploadNotebook(String notebookJson, String fileName);
  JupyterNotebookDTO downloadNotebook(String fileName);
  void deleteNotebook(String fileName);
}

