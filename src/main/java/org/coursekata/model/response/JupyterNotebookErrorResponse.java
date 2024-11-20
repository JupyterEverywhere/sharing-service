package org.coursekata.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class JupyterNotebookErrorResponse implements JupyterNotebookResponse {

    @JsonProperty("error_code")
    private String errorCode;

    private String message;
    private Timestamp timestamp;

    public JupyterNotebookErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
}
