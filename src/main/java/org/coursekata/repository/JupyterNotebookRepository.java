package org.coursekata.repository;

import java.util.List;
import org.coursekata.model.JupyterNotebookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JupyterNotebookRepository extends JpaRepository<JupyterNotebookEntity, UUID> {
  List<JupyterNotebookEntity> findBySessionId(UUID sessionId);
  Optional<JupyterNotebookEntity> findNotebookById(UUID notebookId);
  Optional<JupyterNotebookEntity> findByReadableId(String readableId);
}
