package org.coursekata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.coursekata.dto.JupyterNotebookDTO;
import org.coursekata.dto.MetadataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

  @InjectMocks
  private StorageServiceImpl storageService;

  @Mock(name = "fileStorageService")
  private StorageService fileStorageService;

  @Mock(name = "s3StorageService")
  private StorageService s3StorageService;

  @BeforeEach
  public void setUp() {
    storageService = new StorageServiceImpl(fileStorageService, s3StorageService);
  }

  @Test
  void testUploadNotebook_LocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "local");

    String notebookJson = "{\"notebook\": true}";
    String fileName = "test.ipynb";
    String expectedUrl = "/local/storage/test.ipynb";

    when(fileStorageService.uploadNotebook(notebookJson, fileName)).thenReturn(expectedUrl);

    String result = storageService.uploadNotebook(notebookJson, fileName);

    assertEquals(expectedUrl, result);
    verify(fileStorageService).uploadNotebook(notebookJson, fileName);
    verifyNoInteractions(s3StorageService);
  }

  @Test
  void testUploadNotebook_NonLocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "production");

    String notebookJson = "{\"notebook\": true}";
    String fileName = "test.ipynb";
    String expectedUrl = "s3://bucket/test.ipynb";

    when(s3StorageService.uploadNotebook(notebookJson, fileName)).thenReturn(expectedUrl);

    String result = storageService.uploadNotebook(notebookJson, fileName);

    assertEquals(expectedUrl, result);
    verify(s3StorageService).uploadNotebook(notebookJson, fileName);
    verifyNoInteractions(fileStorageService);
  }

  @Test
  void testDownloadNotebook_LocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "local");

    String fileName = "test.ipynb";
    JupyterNotebookDTO expectedNotebook = new JupyterNotebookDTO();
    MetadataDTO metadata = new MetadataDTO();
    expectedNotebook.setMetadata(metadata);

    when(fileStorageService.downloadNotebook(fileName)).thenReturn(expectedNotebook);

    JupyterNotebookDTO result = storageService.downloadNotebook(fileName);

    assertEquals(expectedNotebook, result);
    verify(fileStorageService).downloadNotebook(fileName);
    verifyNoInteractions(s3StorageService);
  }

  @Test
  void testDownloadNotebook_NonLocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "production");

    String fileName = "test.ipynb";
    JupyterNotebookDTO expectedNotebook = new JupyterNotebookDTO();
    MetadataDTO metadata = new MetadataDTO();
    expectedNotebook.setMetadata(metadata);

    when(s3StorageService.downloadNotebook(fileName)).thenReturn(expectedNotebook);

    JupyterNotebookDTO result = storageService.downloadNotebook(fileName);

    assertEquals(expectedNotebook, result);
    verify(s3StorageService).downloadNotebook(fileName);
    verifyNoInteractions(fileStorageService);
  }

  @Test
  void testDeleteNotebook_LocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "local");

    String fileName = "test.ipynb";

    doNothing().when(fileStorageService).deleteNotebook(fileName);

    storageService.deleteNotebook(fileName);

    verify(fileStorageService).deleteNotebook(fileName);
    verifyNoInteractions(s3StorageService);
  }

  @Test
  void testDeleteNotebook_NonLocalProfile() {
    ReflectionTestUtils.setField(storageService, "activeProfile", "production");

    String fileName = "test.ipynb";

    doNothing().when(s3StorageService).deleteNotebook(fileName);

    storageService.deleteNotebook(fileName);

    verify(s3StorageService).deleteNotebook(fileName);
    verifyNoInteractions(fileStorageService);
  }
}
