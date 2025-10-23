package org.jupytereverywhere;

import org.jupytereverywhere.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class S3SmokeTestRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(S3SmokeTestRunner.class);
  private final StorageService storageService;

  @Autowired
  public S3SmokeTestRunner(StorageService storageService) {
    this.storageService = storageService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!storageService.getClass().getSimpleName().toLowerCase().contains("s3")) {
      log.info("S3 smoke test skipped: not using S3 storage.");
      return;
    }
    String testKey = "smoke-test-object-" + System.currentTimeMillis() + ".txt";
    String testContent = "s3 smoke test";
    try {
      log.info("S3 smoke test: uploading test object {}", testKey);
      storageService.uploadNotebook(testContent, testKey);
      log.info("S3 smoke test: uploaded test object {}", testKey);
      storageService.deleteNotebook(testKey);
      log.info("S3 smoke test: deleted test object {}", testKey);
    } catch (Exception e) {
      log.error("S3 smoke test failed", e);
    }
  }
}
