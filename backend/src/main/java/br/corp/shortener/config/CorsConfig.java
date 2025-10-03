package br.corp.shortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${CORS_ALLOWED_ORIGINS:*}")
  private String allowedOrigins;

  @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:*}")
  private String allowedOriginPatterns;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    final String[] origins = Arrays.stream(allowedOrigins.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toArray(String[]::new);

    final String[] patterns = Arrays.stream(allowedOriginPatterns.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toArray(String[]::new);

    var mapping = registry.addMapping("/**")
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .exposedHeaders("Location")
      .allowCredentials(false)
      .maxAge(3600);

    if (patterns.length > 0) {
      mapping.allowedOriginPatterns(patterns);
    } else if (origins.length == 1 && "*".equals(origins[0])) {
      // Quando '*' é usado, prefira patterns para cobrir todos os casos de CORS
      mapping.allowedOriginPatterns("*");
    } else {
      mapping.allowedOrigins(origins);
    }
  }
}