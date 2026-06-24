package com.allan.task_yard.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebConfig.CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

  private final CorsProperties corsProperties;

  public WebConfig(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(corsProperties.allowedOrigins().split(","))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }

  @Validated
  @ConfigurationProperties("app.cors")
  public record CorsProperties(@NotBlank String allowedOrigins) {
  }
}