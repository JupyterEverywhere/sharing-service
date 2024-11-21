package org.coursekata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.coursekata.model.response.JupyterNotebookRetrieved;
import org.coursekata.model.response.JupyterNotebookSaved;
import org.coursekata.dto.JupyterNotebookDTO;
import org.coursekata.dto.MetadataDTO;
import org.coursekata.exception.InvalidNotebookException;
import org.coursekata.exception.NotebookNotFoundException;
import org.coursekata.exception.NotebookStorageException;
import org.coursekata.exception.SessionMismatchException;
import org.coursekata.model.JupyterNotebookEntity;
import org.coursekata.repository.JupyterNotebookRepository;
import org.coursekata.service.utils.JupyterNotebookValidator;
import org.coursekata.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
  private final ObjectMapper objectMapper;

  @Autowired
  public JupyterNotebookService(
      @Qualifier("storageServiceImpl") StorageService storageService,
      JupyterNotebookValidator jupyterNotebookValidator,
      JupyterNotebookRepository notebookRepository,
      EntityManager entityManager,
      ObjectMapper objectMapper) {
    this.storageService = storageService;
    this.jupyterNotebookValidator = jupyterNotebookValidator;
    this.notebookRepository = notebookRepository;
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
  }

  public JupyterNotebookRetrieved getNotebookContent(UUID notebookId) {
    JupyterNotebookEntity notebookEntity = notebookRepository.findById(notebookId)
        .orElseThrow(() -> {
          log.error(new StringMapMessage()
              .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
          return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
        });

    JupyterNotebookDTO notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
            notebookEntity.getId(),
            notebookEntity.getDomain(),
            notebookEntity.getReadableId(),
            notebookContent
    );
  }

  public JupyterNotebookDTO fetchNotebookContent(JupyterNotebookEntity notebookEntity) {
    try {
      return storageService.downloadNotebook(notebookEntity.getStorageUrl());
    } catch (Exception e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Error fetching notebook content from storage")
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
          .with("Error", e.getMessage()), e);
      throw new NotebookStorageException("Error fetching notebook content from storage", e);
    }
  }

  @Transactional
  public JupyterNotebookSaved uploadNotebook(JupyterNotebookDTO notebookDto, UUID sessionId, String domain)
      throws InvalidNotebookException {

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Validating and storing notebook")
        .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
        .with(DOMAIN_MESSAGE_KEY, domain));

    try {
      JupyterNotebookEntity notebookEntity = validateAndStoreNotebook(notebookDto, sessionId, domain);
      return new JupyterNotebookSaved(notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
    } catch (InvalidNotebookException e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, NOTEBOOK_VALIDATION_FAILED_MESSAGE)
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with(DOMAIN_MESSAGE_KEY, domain), e);
      throw e;
    } catch (Exception e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Error during notebook upload")
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with(DOMAIN_MESSAGE_KEY, domain), e);
      throw new RuntimeException("Error uploading notebook", e);
    }
  }

  public JupyterNotebookEntity validateAndStoreNotebook(JupyterNotebookDTO notebookDto, UUID sessionId, String domain)
      throws InvalidNotebookException, JsonProcessingException {

    validateNotebookMetadata(notebookDto);

    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);

    if (!jupyterNotebookValidator.validateNotebook(notebookJsonString)) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, NOTEBOOK_VALIDATION_FAILED_MESSAGE)
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString()));
      throw new InvalidNotebookException(NOTEBOOK_VALIDATION_FAILED_MESSAGE);
    }

    JupyterNotebookEntity notebookEntity = saveNotebookMetadata(sessionId, notebookDto.getMetadata(), domain);

    String fileName = notebookEntity.getId().toString() + ".ipynb";

    String storageUrl = storeNotebook(notebookDto, fileName);

    notebookEntity.setStorageUrl(storageUrl);
    notebookRepository.save(notebookEntity);

    return notebookEntity;
  }

  public JupyterNotebookSaved updateNotebook(UUID notebookId, JupyterNotebookDTO notebookDto, UUID sessionId)
      throws SessionMismatchException, InvalidNotebookException, JsonProcessingException {

    JupyterNotebookEntity storedNotebook = notebookRepository.findById(notebookId)
        .orElseThrow(() -> {
          log.error(new StringMapMessage()
              .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
          return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
        });

    if (!sessionId.equals(storedNotebook.getSessionId())) {
      log.warn(new StringMapMessage()
          .with(MESSAGE_KEY, "Session ID mismatch")
          .with("StoredSessionId", storedNotebook.getSessionId().toString())
          .with("ProvidedSessionId", sessionId.toString())
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
      throw new SessionMismatchException("Session ID mismatch");
    }

    validateNotebookMetadata(notebookDto);

    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);
    if (!jupyterNotebookValidator.validateNotebook(notebookJsonString)) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Notebook validation failed during update")
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
      throw new InvalidNotebookException(NOTEBOOK_VALIDATION_FAILED_MESSAGE);
    }

    String fileName = storedNotebook.getId().toString() + ".ipynb";

    storeNotebook(notebookDto, fileName);

    updateNotebookMetadata(storedNotebook, notebookDto, sessionId);

    return new JupyterNotebookSaved(storedNotebook.getId(), storedNotebook.getDomain(), storedNotebook.getReadableId());
  }

  private void validateNotebookMetadata(JupyterNotebookDTO notebookDto)
      throws InvalidNotebookException {

    MetadataDTO metadata = notebookDto.getMetadata();
    if (metadata == null) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Invalid metadata format in notebook"));
      throw new InvalidNotebookException("Invalid metadata format in notebook");
    }
    notebookDto.setMetadata(metadata);
  }

  String storeNotebook(JupyterNotebookDTO notebookDto, String fileName) throws JsonProcessingException {
    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);
    return storageService.uploadNotebook(notebookJsonString, fileName);
  }

  JupyterNotebookEntity saveNotebookMetadata(UUID sessionId, MetadataDTO metadata, String domain) {

    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setSessionId(sessionId);
    notebookEntity.setDomain(domain);
    notebookEntity.setStorageUrl("");

    setNotebookEntityMetadata(notebookEntity, metadata);

    Timestamp createdAt = DateUtils.utcDateToTimestamp(Date.from(Instant.now()));

    notebookEntity.setCreatedAt(createdAt);

    JupyterNotebookEntity savedNotebook = notebookRepository.saveAndFlush(notebookEntity);
    entityManager.refresh(savedNotebook);

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Notebook metadata saved in database")
        .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
        .with("CreatedAt", createdAt.toString()));

    return savedNotebook;
  }

  void updateNotebookMetadata(JupyterNotebookEntity notebookEntity, JupyterNotebookDTO notebookDto,
      UUID sessionId) {

    MetadataDTO metadata = notebookDto.getMetadata();
    if (metadata == null) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Metadata is missing in the notebook DTO")
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
      throw new InvalidNotebookException("Metadata is missing");
    }

    notebookEntity.setSessionId(sessionId);
    setNotebookEntityMetadata(notebookEntity, metadata);

    notebookRepository.save(notebookEntity);

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Notebook metadata updated in database")
        .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
  }

  private void setNotebookEntityMetadata(JupyterNotebookEntity notebookEntity, MetadataDTO metadata) {
    if (metadata.getKernelspec() != null) {
      notebookEntity.setKernelName(metadata.getKernelspec().getName());
      notebookEntity.setKernelDisplayName(metadata.getKernelspec().getDisplayName());
    }

    if (metadata.getLanguageInfo() != null) {
      notebookEntity.setLanguage(metadata.getLanguageInfo().getName());
      notebookEntity.setLanguageVersion(metadata.getLanguageInfo().getVersion());
      notebookEntity.setFileExtension(metadata.getLanguageInfo().getFileExtension());
    }
  }

  public JupyterNotebookRetrieved getNotebookContent(String readableId) {
    JupyterNotebookEntity notebookEntity = notebookRepository.findByReadableId(readableId)
            .orElseThrow(() -> {
              log.error(new StringMapMessage()
                      .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                      .with("ReadableId", readableId)
              );
              return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
            });

    JupyterNotebookDTO notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
            notebookEntity.getId(),
            notebookEntity.getDomain(),
            notebookEntity.getReadableId(),
            notebookContent
    );
  }

  public JupyterNotebookSaved updateNotebook(String readableId, JupyterNotebookDTO notebookDto, UUID sessionId) throws JsonProcessingException {
    JupyterNotebookEntity notebookEntity = notebookRepository.findByReadableId(readableId)
            .orElseThrow(() -> {
              log.error(new StringMapMessage()
                      .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                      .with("ReadableId", readableId)
              );
              return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
            });

    updateNotebook(notebookEntity.getId(), notebookDto, sessionId);

    return new JupyterNotebookSaved(notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
  }
}
