package com.leadersfault.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationInsightsConfig {

  @Bean
  @ConditionalOnMissingBean
  public TelemetryClient telemetryClient() {
    return new TelemetryClient();
  }
}
