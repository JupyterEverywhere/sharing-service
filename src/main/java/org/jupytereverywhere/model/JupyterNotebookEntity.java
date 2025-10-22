package org.jupytereverywhere.model;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "jupyter_notebooks_metadata")
@Data
public class JupyterNotebookEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false)
  private UUID sessionId;

  private String kernelName;

  private String kernelDisplayName;

  private String language;

  private String languageVersion;

  private String fileExtension;

  @Column(nullable = false)
  private String domain;

  @Column(nullable = false)
  private String storageUrl;

  private String readableId;

  @ToString.Exclude
  private String password;

  @Column(nullable = false)
  private Timestamp createdAt;
}
