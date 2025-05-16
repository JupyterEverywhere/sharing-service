package org.jupytereverywhere.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.jupytereverywhere.model.JupyterNotebookEntity;

public interface JupyterNotebookRepository extends JpaRepository<JupyterNotebookEntity, UUID> {
  List<JupyterNotebookEntity> findBySessionId(UUID sessionId);
  Optional<JupyterNotebookEntity> findNotebookById(UUID notebookId);
  Optional<JupyterNotebookEntity> findByReadableId(String readableId);
}
