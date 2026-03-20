package org.jupytereverywhere.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface StorageService {
  String uploadNotebook(String notebookJson, String fileName);

  String downloadNotebookAsJson(String fileName);

  void deleteNotebook(String fileName);

  void deleteNotebooks(List<String> fileNames);
}
