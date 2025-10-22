package org.jupytereverywhere.model.response;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JupyterNotebookErrorResponse implements JupyterNotebookResponse {

    @JsonProperty("error_code")
    private String errorCode;

    private String message;
    private Timestamp timestamp;

    @JsonProperty("details")
    private Map<String, Object> details;

    public JupyterNotebookErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public JupyterNotebookErrorResponse(String errorCode, String message, Map<String, Object> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.details = details;
    }

    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }
}
