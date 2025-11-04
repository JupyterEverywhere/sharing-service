package org.jupytereverywhere.service;

import org.springframework.stereotype.Service;

@Service
public interface StorageService {
  String uploadNotebook(String notebookJson, String fileName);

  String downloadNotebookAsJson(String fileName);

  void deleteNotebook(String fileName);
}
