package org.coursekata.config;

public class SecurityConstants {
  private SecurityConstants() {}
  protected static final String[] PUBLIC_URLS = {
      ApiConstants.API_BASE_URL + "/auth/issue",
      ApiConstants.API_BASE_URL + "/auth/refresh"
  };
}
