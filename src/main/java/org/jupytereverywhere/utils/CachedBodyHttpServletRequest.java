package org.jupytereverywhere.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Wrapper for HttpServletRequest that caches the request body for multiple reads.
 *
 * <p>Spring MVC normally only allows the request body to be read once. This wrapper caches the body
 * during the first read, allowing it to be accessed multiple times. This is essential for
 * validating the raw incoming JSON before Spring deserializes it.
 *
 * <p>Security: This wrapper enforces a maximum body size of 15MB to prevent memory exhaustion
 * attacks, providing defense-in-depth alongside application-level size validation.
 *
 * <p>Usage: Wrap the request in a CachedBodyFilter before passing it to the controller.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  /** Maximum size for cached request body (15MB) - matches MAX_IN_MEMORY_SIZE */
  private static final long MAX_CACHED_BODY_SIZE = 15 * 1024 * 1024;

  private final byte[] cachedBody;

  /**
   * Constructs a new CachedBodyHttpServletRequest.
   *
   * @param request the original HTTP request
   * @throws IOException if reading the request body fails or exceeds size limit
   */
  public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    this.cachedBody = readAndValidateBody(request.getInputStream());
  }

  /**
   * Reads the request body with size validation to prevent memory exhaustion.
   *
   * @param inputStream the request input stream
   * @return the cached body bytes
   * @throws IOException if reading fails or body exceeds MAX_CACHED_BODY_SIZE
   */
  private byte[] readAndValidateBody(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[4096];
    int bytesRead;
    long totalBytesRead = 0;
    java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      totalBytesRead += bytesRead;
      if (totalBytesRead > MAX_CACHED_BODY_SIZE) {
        throw new IOException(
            String.format(
                "Request body size (%d bytes) exceeds maximum cacheable size of %d bytes",
                totalBytesRead, MAX_CACHED_BODY_SIZE));
      }
      outputStream.write(buffer, 0, bytesRead);
    }

    return outputStream.toByteArray();
  }

  /**
   * Gets the cached request body as a String.
   *
   * @return the request body as UTF-8 string
   */
  public String getCachedBody() {
    return new String(cachedBody, StandardCharsets.UTF_8);
  }

  @Override
  public ServletInputStream getInputStream() {
    return new CachedBodyServletInputStream(this.cachedBody);
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(
        new InputStreamReader(new ByteArrayInputStream(this.cachedBody), StandardCharsets.UTF_8));
  }

  /** Custom ServletInputStream that reads from the cached byte array. */
  private static class CachedBodyServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream byteArrayInputStream;

    public CachedBodyServletInputStream(byte[] cachedBody) {
      this.byteArrayInputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public int read() {
      return byteArrayInputStream.read();
    }

    @Override
    public boolean isFinished() {
      return byteArrayInputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException(
          "ReadListener not supported in CachedBodyServletInputStream");
    }
  }
}
