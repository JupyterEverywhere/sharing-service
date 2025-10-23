package org.jupytereverywhere.filter;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RequestSizeLimitFilterTest {

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @InjectMocks private RequestSizeLimitFilter filter;

  private static final long MAX_NOTEBOOK_SIZE_BYTES = 10485760L; // 10 MB

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(filter, "maxNotebookSizeBytes", MAX_NOTEBOOK_SIZE_BYTES);
  }

  @Test
  void testFilterAllowsRequestWithinSizeLimit() throws ServletException, IOException {
    // Request size: 5 MB (well within limit)
    when(request.getHeader("Content-Length")).thenReturn("5242880");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterAllowsRequestAtExactLimit() throws ServletException, IOException {
    // Request size: exactly 10 MB (notebook limit, within buffered limit)
    when(request.getHeader("Content-Length")).thenReturn(String.valueOf(MAX_NOTEBOOK_SIZE_BYTES));

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterAllowsRequestWithinBufferedLimit() throws ServletException, IOException {
    // Request size: 14 MB (above notebook limit but within buffered limit of 15 MB)
    when(request.getHeader("Content-Length")).thenReturn("14680064");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterRejectsRequestExceedingBufferedLimit() throws ServletException, IOException {
    // Request size: 16 MB (exceeds buffered limit of 15 MB)
    when(request.getHeader("Content-Length")).thenReturn("16777216");
    when(request.getRequestURI()).thenReturn("/notebooks");
    when(request.getMethod()).thenReturn("POST");

    filter.doFilterInternal(request, response, filterChain);

    verify(response, times(1))
        .sendError(eq(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE), contains("Request size"));
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void testFilterRejectsVeryLargeRequest() throws ServletException, IOException {
    // Request size: 100 MB (way over limit)
    when(request.getHeader("Content-Length")).thenReturn("104857600");
    when(request.getRequestURI()).thenReturn("/notebooks");
    when(request.getMethod()).thenReturn("POST");

    filter.doFilterInternal(request, response, filterChain);

    verify(response, times(1))
        .sendError(eq(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE), contains("Request size"));
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void testFilterAllowsRequestWithoutContentLength() throws ServletException, IOException {
    // No Content-Length header (e.g., chunked encoding or GET request)
    when(request.getHeader("Content-Length")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterAllowsRequestWithEmptyContentLength() throws ServletException, IOException {
    // Empty Content-Length header
    when(request.getHeader("Content-Length")).thenReturn("");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterHandlesInvalidContentLengthGracefully() throws ServletException, IOException {
    // Invalid Content-Length header (non-numeric)
    when(request.getHeader("Content-Length")).thenReturn("invalid");

    filter.doFilterInternal(request, response, filterChain);

    // Should continue processing (let downstream validation handle it)
    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void testFilterHandlesZeroContentLength() throws ServletException, IOException {
    // Content-Length: 0 (empty request body)
    when(request.getHeader("Content-Length")).thenReturn("0");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }
}
