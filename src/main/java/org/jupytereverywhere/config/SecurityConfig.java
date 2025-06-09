package org.jupytereverywhere.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.jupytereverywhere.filter.JwtRequestFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtRequestFilter jwtRequestFilter;

  @Value("${cors.enabled:false}")
  private boolean corsEnabled;
  
  @Value("${cors.allowed-origins:}")
  private String allowedOrigins;
  
  @Value("${cors.allowed-methods:}")
  private String allowedMethods;
  
  @Value("${cors.allowed-headers:}")
  private String allowedHeaders;
  
  @Value("${cors.exposed-headers:}")
  private String exposedHeaders;
  
  @Value("${cors.allow-credentials:true}")
  private boolean allowCredentials;
  
  @Value("${cors.max-age:3600}")
  private long maxAge;

  public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
    this.jwtRequestFilter = jwtRequestFilter;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    // TODO: CORS is untested
    if (!corsEnabled) {
      return null; // This will use Spring Security's default CORS handling
    }
    
    CorsConfiguration configuration = new CorsConfiguration();
    
    // origins
    if (!allowedOrigins.isEmpty()) {
      configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    } else {
      configuration.addAllowedOriginPattern("*");
    }
    
    // methods
    if (!allowedMethods.isEmpty()) {
      configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    } else {
      configuration.addAllowedMethod("*");
    }
    
    // headers
    if (!allowedHeaders.isEmpty()) {
      configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
    } else {
      configuration.addAllowedHeader("*");
    }
    
    // exposed headers
    if (!exposedHeaders.isEmpty()) {
      configuration.setExposedHeaders(Arrays.asList(exposedHeaders.split(",")));
    } else {
      // Default exposed headers that are commonly needed
      configuration.addExposedHeader("Authorization");
      configuration.addExposedHeader("Content-Type");
      configuration.addExposedHeader("X-Requested-With");
      configuration.addExposedHeader("Accept");
      configuration.addExposedHeader("Origin");
      configuration.addExposedHeader("Access-Control-Request-Method");
      configuration.addExposedHeader("Access-Control-Request-Headers");
    }
    
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAge);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    CorsConfigurationSource corsSource = corsConfigurationSource();

    http
        .cors(corsSource != null ? cors -> cors.configurationSource(corsSource) : withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
