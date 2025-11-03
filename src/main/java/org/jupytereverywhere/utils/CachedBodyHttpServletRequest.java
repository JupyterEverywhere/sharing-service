package org.jupytereverywhere.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.util.StreamUtils;

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
 * <p>Usage: Wrap the request in a CachedBodyFilter before passing it to the controller.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] cachedBody;

  /**
   * Constructs a new CachedBodyHttpServletRequest.
   *
   * @param request the original HTTP request
   * @throws IOException if reading the request body fails
   */
  public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
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
