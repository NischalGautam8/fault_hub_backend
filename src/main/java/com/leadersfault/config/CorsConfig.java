package com.leadersfault.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/**")
      .allowedOrigins(
        "http://localhost:3000",
        "http://localhost:3001",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:3001",
        "http://localhost:8080",
        "http://127.0.0.1:8080",
        "https://www.nischal.tech",
        "https://wwww.faulthub.nischal.tech",
        "http://www.nischal.tech",
        "https://fault-hub-frontend-1swf-74s1ajj7h-headshighs-projects.vercel.app",
        "www.nischal.tech",
        "https://www.faulthub.nischal.tech"
      )
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
      .allowedHeaders("*")
      .exposedHeaders(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin"
      )
      .allowCredentials(true)
      .maxAge(3600);
  }
}
