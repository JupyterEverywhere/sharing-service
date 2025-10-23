package org.jupytereverywhere.service;

import org.springframework.stereotype.Service;

import org.jupytereverywhere.dto.JupyterNotebookDTO;

@Service
public interface StorageService {
  String uploadNotebook(String notebookJson, String fileName);
  JupyterNotebookDTO downloadNotebook(String fileName);
  void deleteNotebook(String fileName);
}
