package org.jupytereverywhere.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

/**
 * Servlet filter that validates request size BEFORE request body deserialization.
 * This provides early rejection of oversized requests to prevent memory exhaustion.
 *
 * Executes with HIGHEST_PRECEDENCE to run before JWT authentication and request body parsing.
 */
@Log4j2
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final String MESSAGE_KEY = "Message";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    // Buffer factor to account for JSON overhead and HTTP headers (50% margin)
    private static final double SIZE_BUFFER_FACTOR = 1.5;

    @Value("${notebook.max-size-bytes}")
    private long maxNotebookSizeBytes;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String contentLengthHeader = request.getHeader(CONTENT_LENGTH_HEADER);

        if (contentLengthHeader != null && !contentLengthHeader.isEmpty()) {
            try {
                long requestSize = Long.parseLong(contentLengthHeader);
                long maxAllowedSize = (long) (maxNotebookSizeBytes * SIZE_BUFFER_FACTOR);

                if (requestSize > maxAllowedSize) {
                    long maxSizeMB = maxNotebookSizeBytes / (1024 * 1024);
                    String errorMessage = String.format(
                        "Request size (%d bytes) exceeds maximum allowed size of %d MB",
                        requestSize, maxSizeMB);

                    log.warn(new StringMapMessage()
                        .with(MESSAGE_KEY, errorMessage)
                        .with("RequestSize", String.valueOf(requestSize))
                        .with("MaxAllowedSize", String.valueOf(maxAllowedSize))
                        .with("MaxNotebookSizeMB", String.valueOf(maxSizeMB))
                        .with("RequestURI", request.getRequestURI())
                        .with("Method", request.getMethod()));

                    response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        errorMessage);
                    return;
                }

                log.debug(new StringMapMessage()
                    .with(MESSAGE_KEY, "Request size validation passed")
                    .with("RequestSize", String.valueOf(requestSize))
                    .with("MaxAllowedSize", String.valueOf(maxAllowedSize)));

            } catch (NumberFormatException e) {
                log.warn(new StringMapMessage()
                    .with(MESSAGE_KEY, "Invalid Content-Length header")
                    .with("ContentLength", contentLengthHeader)
                    .with("ErrorMessage", e.getMessage()));
                // Continue processing - let downstream validation handle it
            }
        }

        chain.doFilter(request, response);
    }
}
