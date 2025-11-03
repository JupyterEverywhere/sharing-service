package org.jupytereverywhere.filter;

import java.io.IOException;

import org.jupytereverywhere.utils.CachedBodyHttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that caches HTTP request bodies for notebook endpoints.
 *
 * <p>This filter wraps POST and PUT requests to /notebooks endpoints with
 * CachedBodyHttpServletRequest, which allows the request body to be read multiple times. This is
 * necessary to: 1. Validate the raw incoming JSON against the nbformat schema 2. Allow Spring MVC
 * to deserialize the same JSON into DTOs
 *
 * <p>The filter runs at HIGHEST_PRECEDENCE + 2, after RequestSizeLimitFilter (which prevents
 * caching of oversized requests) but before JwtRequestFilter.
 *
 * <p>Note: This filter only caches bodies for notebook-related endpoints to minimize memory usage.
 * Other endpoints use the standard request handling.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class CachedBodyFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (shouldCacheBody(request)) {
      CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
      filterChain.doFilter(cachedRequest, response);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  /**
   * Determines if the request body should be cached.
   *
   * @param request the HTTP request
   * @return true if this is a POST or PUT request to a notebooks endpoint
   */
  private boolean shouldCacheBody(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();

    // Only cache for POST/PUT to notebook endpoints
    boolean isModifyingRequest = "POST".equals(method) || "PUT".equals(method);
    boolean isNotebookEndpoint = path != null && path.contains("/notebooks");

    return isModifyingRequest && isNotebookEndpoint;
  }
}
