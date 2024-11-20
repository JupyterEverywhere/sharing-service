package org.coursekata.service;

import jakarta.annotation.Nullable;
import org.coursekata.dto.JupyterNotebookDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service("storageServiceImpl")
public class StorageServiceImpl implements StorageService {

  private final StorageService fileStorageService;
  private final StorageService s3StorageService;

  @Value("${spring.profiles.active}")
  private String activeProfile;

  @Autowired
  public StorageServiceImpl(
      @Qualifier("fileStorageService") StorageService fileStorageService,
      @Qualifier("s3StorageService") @Nullable StorageService s3StorageService) {
    this.fileStorageService = fileStorageService;
    this.s3StorageService = s3StorageService;
  }

  private boolean isLocalProfile() {
    return "local".equalsIgnoreCase(activeProfile) || "docker".equalsIgnoreCase(activeProfile);
  }

  @Override
  public String uploadNotebook(String notebookJson, String fileName) {
    if (isLocalProfile()) {
      return fileStorageService.uploadNotebook(notebookJson, fileName);
    } else {
      assert s3StorageService != null;
      return s3StorageService.uploadNotebook(notebookJson, fileName);
    }
  }

  @Override
  public JupyterNotebookDTO downloadNotebook(String fileName) {
    if (isLocalProfile()) {
      return fileStorageService.downloadNotebook(fileName);
    } else {
      assert s3StorageService != null;
      return s3StorageService.downloadNotebook(fileName);
    }
  }

  @Override
  public void deleteNotebook(String fileName) {
    if (isLocalProfile()) {
      fileStorageService.deleteNotebook(fileName);
    } else {
      assert s3StorageService != null;
      s3StorageService.deleteNotebook(fileName);
    }
  }
}
