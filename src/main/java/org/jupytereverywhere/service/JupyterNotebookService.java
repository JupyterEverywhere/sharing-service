package org.jupytereverywhere.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.dto.MetadataDTO;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.NotebookStorageException;
import org.jupytereverywhere.exception.NotebookTooLargeException;
import org.jupytereverywhere.exception.UnauthorizedNotebookAccessException;
import org.jupytereverywhere.model.JupyterNotebookEntity;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.repository.JupyterNotebookRepository;
import org.jupytereverywhere.service.utils.JupyterNotebookValidator;
import org.jupytereverywhere.utils.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class JupyterNotebookService {

  public static final String MESSAGE_KEY = "Message";
  public static final String NOTEBOOK_ID_MESSAGE_KEY = "NotebookID";
  public static final String SESSION_ID_MESSAGE_KEY = "SessionId";
  public static final String DOMAIN_MESSAGE_KEY = "Domain";

  private static final String NOTEBOOK_NOT_FOUND_MESSAGE = "Notebook not found";
  public static final String NOTEBOOK_VALIDATION_FAILED_MESSAGE = "Notebook validation failed";

  private final StorageService storageService;
  private final JupyterNotebookValidator jupyterNotebookValidator;
  private final JupyterNotebookRepository notebookRepository;
  private final EntityManager entityManager;
  private final TransactionTemplate transactionTemplate;

  private final JwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;

  @Value("${notebook.max-size-bytes}")
  private long maxNotebookSizeBytes;

  public JupyterNotebookService(
      StorageService storageService,
      JupyterNotebookValidator jupyterNotebookValidator,
      JupyterNotebookRepository notebookRepository,
      EntityManager entityManager,
      JwtTokenService jwtTokenService,
      PasswordEncoder passwordEncoder,
      PlatformTransactionManager transactionManager) {
    this.storageService = storageService;
    this.jupyterNotebookValidator = jupyterNotebookValidator;
    this.notebookRepository = notebookRepository;
    this.entityManager = entityManager;
    this.jwtTokenService = jwtTokenService;
    this.passwordEncoder = passwordEncoder;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public JupyterNotebookRetrieved getNotebookContent(UUID notebookId) {
    JupyterNotebookEntity notebookEntity =
        notebookRepository
            .findById(notebookId)
            .orElseThrow(
                () -> {
                  log.error(
                      new StringMapMessage()
                          .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
                  return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
                });

    String notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
        notebookEntity.getId(),
        notebookEntity.getDomain(),
        notebookEntity.getReadableId(),
        notebookContent);
  }

  public String fetchNotebookContent(JupyterNotebookEntity notebookEntity) {
    try {
      return storageService.downloadNotebookAsJson(notebookEntity.getStorageUrl());
    } catch (Exception e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Error fetching notebook content from storage")
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
              .with("Error", e.getMessage()),
          e);
      throw new NotebookStorageException("Error fetching notebook content from storage", e);
    }
  }

  @Transactional
  public JupyterNotebookSaved uploadNotebook(
      JupyterNotebookRequest jupyterNotebookRequest,
      UUID sessionId,
      String domain,
      byte[] rawNotebookBytes)
      throws InvalidNotebookException {

    JupyterNotebookDTO notebookDto = jupyterNotebookRequest.getNotebook();
    if (notebookDto == null) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Notebook DTO is null")
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with(DOMAIN_MESSAGE_KEY, domain));
      throw new InvalidNotebookException("Notebook field is required and cannot be null");
    }

    String password = jupyterNotebookRequest.getPassword();

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Validating and storing notebook")
            .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
            .with(DOMAIN_MESSAGE_KEY, domain));

    try {

      JupyterNotebookEntity notebookEntity =
          validateAndStoreNotebook(notebookDto, sessionId, domain, password, rawNotebookBytes);

      return new JupyterNotebookSaved(
          notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
    } catch (NotebookTooLargeException e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Notebook size exceeds limit")
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with(DOMAIN_MESSAGE_KEY, domain),
          e);
      throw e;
    } catch (InvalidNotebookException e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, NOTEBOOK_VALIDATION_FAILED_MESSAGE)
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with(DOMAIN_MESSAGE_KEY, domain),
          e);
      throw e;
    } catch (Exception e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Error during notebook upload")
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with(DOMAIN_MESSAGE_KEY, domain)
              .with("Error", e.getMessage()));
      throw new RuntimeException("Error uploading notebook", e);
    }
  }

  public JupyterNotebookEntity validateAndStoreNotebook(
      JupyterNotebookDTO notebookDto,
      UUID sessionId,
      String domain,
      String password,
      byte[] rawNotebookBytes)
      throws InvalidNotebookException, JsonProcessingException {

    // Validate the raw incoming bytes to preserve user's exact input
    validateNotebookSize(rawNotebookBytes, sessionId);

    // Validator throws InvalidNotebookException on failure, returns parsed JsonNode on success
    jupyterNotebookValidator.validateNotebook(rawNotebookBytes);

    JupyterNotebookEntity notebookEntity =
        saveNotebookMetadata(sessionId, notebookDto.getMetadata(), domain, password);

    String fileName = notebookEntity.getId().toString() + ".ipynb";

    // Store the raw bytes (not re-serialized) to preserve user's exact input
    String storageUrl = storeNotebook(rawNotebookBytes, fileName);

    notebookEntity.setStorageUrl(storageUrl);
    notebookRepository.save(notebookEntity);

    return notebookEntity;
  }

  @Transactional
  public JupyterNotebookSaved updateNotebook(
      UUID notebookId,
      JupyterNotebookDTO notebookDto,
      UUID sessionId,
      String token,
      byte[] rawNotebookBytes)
      throws UnauthorizedNotebookAccessException,
          InvalidNotebookException,
          JsonProcessingException {

    JupyterNotebookEntity storedNotebook =
        notebookRepository
            .findById(notebookId)
            .orElseThrow(
                () -> new NotebookNotFoundException("Notebook not found with ID: " + notebookId));

    Map<String, String> commonLogDetails =
        Map.of(
            NOTEBOOK_ID_MESSAGE_KEY,
            notebookId.toString(),
            "StoredSessionId",
            storedNotebook.getSessionId().toString(),
            "ProvidedSessionId",
            sessionId.toString());

    if (sessionId.equals(storedNotebook.getSessionId())) {
      logInfo("Session IDs match", commonLogDetails);
    } else {
      logInfo("Session ID mismatch", commonLogDetails);

      String notebookIdFromToken = jwtTokenService.extractNotebookIdFromToken(token);

      if (notebookIdFromToken == null) {
        logInfo(
            "Notebook ID missing in token", Map.of(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
        throw new UnauthorizedNotebookAccessException(
            "You do not have permission to update this notebook");
      }

      if (!notebookId.equals(UUID.fromString(notebookIdFromToken))) {
        logInfo(
            "Unauthorized notebook update attempt",
            Map.of(
                NOTEBOOK_ID_MESSAGE_KEY,
                notebookId.toString(),
                "NotebookIdFromToken",
                notebookIdFromToken));
        throw new UnauthorizedNotebookAccessException(
            "You do not have permission to update this notebook");
      }

      logInfo(
          "Notebook ID validation succeeded",
          Map.of(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
    }

    // Validate the raw incoming bytes to preserve user's exact input
    validateNotebookSize(rawNotebookBytes, sessionId);

    // Validator throws InvalidNotebookException on failure
    jupyterNotebookValidator.validateNotebook(rawNotebookBytes);

    String fileName = storedNotebook.getId().toString() + ".ipynb";
    // Store the raw bytes (not re-serialized) to preserve user's exact input
    storeNotebook(rawNotebookBytes, fileName);

    updateNotebookMetadata(storedNotebook, notebookDto, sessionId);

    return new JupyterNotebookSaved(
        storedNotebook.getId(), storedNotebook.getDomain(), storedNotebook.getReadableId());
  }

  void validateNotebookSize(byte[] notebookBytes, UUID sessionId) {
    long notebookSizeBytes = notebookBytes.length;

    if (notebookSizeBytes > maxNotebookSizeBytes) {
      long maxSizeMB = maxNotebookSizeBytes / (1024 * 1024);
      String errorMessage =
          String.format(
              "Notebook size (%d bytes) exceeds maximum allowed size of %d MB",
              notebookSizeBytes, maxSizeMB);

      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, errorMessage)
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with("NotebookSizeBytes", String.valueOf(notebookSizeBytes))
              .with("MaxSizeBytes", String.valueOf(maxNotebookSizeBytes)));

      throw new NotebookTooLargeException(errorMessage, notebookSizeBytes, maxNotebookSizeBytes);
    }
  }

  String storeNotebook(byte[] notebookBytes, String fileName) {
    return storageService.uploadNotebook(notebookBytes, fileName);
  }

  JupyterNotebookEntity saveNotebookMetadata(
      UUID sessionId, MetadataDTO metadata, String domain, String password) {

    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setSessionId(sessionId);
    notebookEntity.setDomain(domain);
    notebookEntity.setStorageUrl("");

    if (password != null && !password.isEmpty()) {
      notebookEntity.setPassword(passwordEncoder.encode(password));
    }

    setNotebookEntityMetadata(notebookEntity, metadata);

    Timestamp createdAt = DateUtils.utcDateToTimestamp(Date.from(Instant.now()));

    notebookEntity.setCreatedAt(createdAt);

    JupyterNotebookEntity savedNotebook = notebookRepository.saveAndFlush(notebookEntity);
    entityManager.refresh(savedNotebook);

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Notebook metadata saved in database")
            .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
            .with("CreatedAt", createdAt.toString()));

    return savedNotebook;
  }

  void updateNotebookMetadata(
      JupyterNotebookEntity notebookEntity, JupyterNotebookDTO notebookDto, UUID sessionId) {

    MetadataDTO metadata = notebookDto.getMetadata();
    if (metadata == null) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Metadata is missing in the notebook DTO")
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
      throw new InvalidNotebookException("Metadata is missing");
    }

    notebookEntity.setSessionId(sessionId);
    setNotebookEntityMetadata(notebookEntity, metadata);

    notebookRepository.save(notebookEntity);

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Notebook metadata updated in database")
            .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
  }

  private void setNotebookEntityMetadata(
      JupyterNotebookEntity notebookEntity, MetadataDTO metadata) {
    if (metadata.getKernelspec() != null) {
      notebookEntity.setKernelName(metadata.getKernelspec().getName());
      notebookEntity.setKernelDisplayName(metadata.getKernelspec().getDisplayName());
    }

    if (metadata.getLanguageInfo() != null) {
      // Set language name if present and non-empty, otherwise leave as null
      String languageName = metadata.getLanguageInfo().getName();
      if (languageName != null && !languageName.trim().isEmpty()) {
        notebookEntity.setLanguage(languageName);
      }

      // Optional fields - only set if present
      if (metadata.getLanguageInfo().getVersion() != null) {
        notebookEntity.setLanguageVersion(metadata.getLanguageInfo().getVersion());
      }
      if (metadata.getLanguageInfo().getFileExtension() != null) {
        notebookEntity.setFileExtension(metadata.getLanguageInfo().getFileExtension());
      }
    }
  }

  public JupyterNotebookRetrieved getNotebookContent(String readableId) {
    JupyterNotebookEntity notebookEntity =
        notebookRepository
            .findByReadableId(readableId)
            .orElseThrow(
                () -> {
                  log.error(
                      new StringMapMessage()
                          .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                          .with("ReadableId", readableId));
                  return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
                });

    String notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
        notebookEntity.getId(),
        notebookEntity.getDomain(),
        notebookEntity.getReadableId(),
        notebookContent);
  }

  public JupyterNotebookSaved updateNotebook(
      String readableId,
      JupyterNotebookDTO notebookDto,
      UUID sessionId,
      String token,
      byte[] rawNotebookBytes)
      throws JsonProcessingException {
    JupyterNotebookEntity notebookEntity =
        notebookRepository
            .findByReadableId(readableId)
            .orElseThrow(
                () -> {
                  log.error(
                      new StringMapMessage()
                          .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                          .with("ReadableId", readableId));
                  return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
                });

    updateNotebook(notebookEntity.getId(), notebookDto, sessionId, token, rawNotebookBytes);

    return new JupyterNotebookSaved(
        notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
  }

  public int deleteNotebooksBySessionId(UUID sessionId, String adminTokenName) {
    // Phase 1: Find and delete metadata within a transaction
    Map<UUID, String> deletedNotebooks =
        transactionTemplate.execute(
            status -> {
              List<JupyterNotebookEntity> notebooks = notebookRepository.findBySessionId(sessionId);
              Map<UUID, String> entries = new java.util.LinkedHashMap<>();
              for (JupyterNotebookEntity notebook : notebooks) {
                entries.put(notebook.getId(), notebook.getStorageUrl());
              }
              if (!notebooks.isEmpty()) {
                notebookRepository.deleteAllInBatch(notebooks);
              }
              return entries;
            });

    int count = deletedNotebooks.size();

    // Phase 2: Best-effort storage cleanup after DB transaction commits
    deletedNotebooks.forEach(
        (notebookId, storageUrl) -> {
          try {
            storageService.deleteNotebook(storageUrl);
          } catch (Exception e) {
            log.warn(
                new StringMapMessage()
                    .with(
                        MESSAGE_KEY,
                        "Failed to delete storage file after successful metadata deletion")
                    .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString())
                    .with("StorageUrl", storageUrl));
          }
        });

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Bulk session notebooks deleted by admin")
            .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
            .with("AdminTokenName", adminTokenName)
            .with("DeletedCount", String.valueOf(count)));

    return count;
  }

  public void deleteNotebook(UUID notebookId, String adminTokenName) {
    // Phase 1: Find and delete metadata within a transaction
    JupyterNotebookEntity deletedEntity =
        transactionTemplate.execute(
            status -> {
              JupyterNotebookEntity entity =
                  notebookRepository
                      .findById(notebookId)
                      .orElseThrow(
                          () -> {
                            log.error(
                                new StringMapMessage()
                                    .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                                    .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
                            return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
                          });
              notebookRepository.deleteById(notebookId);
              return entity;
            });

    // Phase 2: Best-effort storage cleanup after DB transaction commits
    cleanupStorageAfterDelete(deletedEntity.getStorageUrl(), notebookId.toString());

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Notebook deleted by admin")
            .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString())
            .with("ReadableId", deletedEntity.getReadableId())
            .with("AdminTokenName", adminTokenName));
  }

  public void deleteNotebookByReadableId(String readableId, String adminTokenName) {
    // Phase 1: Find and delete metadata within a transaction
    JupyterNotebookEntity deletedEntity =
        transactionTemplate.execute(
            status -> {
              JupyterNotebookEntity entity =
                  notebookRepository
                      .findByReadableId(readableId)
                      .orElseThrow(
                          () -> {
                            log.error(
                                new StringMapMessage()
                                    .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                                    .with("ReadableId", readableId));
                            return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
                          });
              notebookRepository.deleteById(entity.getId());
              return entity;
            });

    // Phase 2: Best-effort storage cleanup after DB transaction commits
    cleanupStorageAfterDelete(deletedEntity.getStorageUrl(), deletedEntity.getId().toString());

    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Notebook deleted by admin")
            .with(NOTEBOOK_ID_MESSAGE_KEY, deletedEntity.getId().toString())
            .with("ReadableId", readableId)
            .with("AdminTokenName", adminTokenName));
  }

  private void cleanupStorageAfterDelete(String storageUrl, String notebookId) {
    try {
      storageService.deleteNotebook(storageUrl);
    } catch (Exception e) {
      log.warn(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Failed to delete storage file after successful metadata deletion")
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId)
              .with("StorageUrl", storageUrl));
    }
  }

  public JupyterNotebookEntity getNotebookById(UUID notebookId) {
    return notebookRepository
        .findNotebookById(notebookId)
        .orElseThrow(
            () -> new NotebookNotFoundException("Notebook not found with ID: " + notebookId));
  }

  private void logInfo(String message, Map<String, String> details) {
    StringMapMessage logMessage = new StringMapMessage().with(MESSAGE_KEY, message);
    details.forEach(logMessage::with);
    log.info(logMessage);
  }
}
