package com.leadersfault.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelemetryLogger {

  private static final Logger logger = LoggerFactory.getLogger(
    TelemetryLogger.class
  );
  private final TelemetryClient telemetryClient;

  public TelemetryLogger(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  public void trackEvent(String eventName) {
    try {
      telemetryClient.trackEvent(eventName);
      logger.info("Tracked event: {}", eventName);
    } catch (Exception e) {
      logger.error("Failed to track event: {}", eventName, e);
    }
  }

  public void trackEvent(String eventName, Map<String, String> properties) {
    try {
      telemetryClient.trackEvent(eventName, properties, null);
      logger.info(
        "Tracked event: {} with properties: {}",
        eventName,
        properties
      );
    } catch (Exception e) {
      logger.error(
        "Failed to track event: {} with properties: {}",
        eventName,
        properties,
        e
      );
    }
  }

  public void trackException(Exception exception) {
    try {
      telemetryClient.trackException(exception);
      logger.error("Tracked exception", exception);
    } catch (Exception e) {
      logger.error("Failed to track exception", e);
    }
  }

  public void trackTrace(String message, SeverityLevel severityLevel) {
    try {
      telemetryClient.trackTrace(message, severityLevel);
      logger.info(
        "Tracked trace: {} with severity: {}",
        message,
        severityLevel
      );
    } catch (Exception e) {
      logger.error("Failed to track trace: {}", message, e);
    }
  }

  public void trackTrace(
    String message,
    SeverityLevel severityLevel,
    Map<String, String> properties
  ) {
    try {
      telemetryClient.trackTrace(message, severityLevel, properties);
      logger.info(
        "Tracked trace: {} with severity: {} and properties: {}",
        message,
        severityLevel,
        properties
      );
    } catch (Exception e) {
      logger.error(
        "Failed to track trace: {} with properties: {}",
        message,
        properties,
        e
      );
    }
  }

  public void trackMetric(String name, double value) {
    try {
      telemetryClient.trackMetric(name, value);
      logger.info("Tracked metric: {} with value: {}", name, value);
    } catch (Exception e) {
      logger.error("Failed to track metric: {}", name, e);
    }
  }

  public void trackMetric(
    String name,
    double value,
    Map<String, String> properties
  ) {
    try {
      // The Application Insights SDK doesn't support properties for metrics directly
      // We'll track the metric without properties and log the properties separately
      telemetryClient.trackMetric(name, value);
      logger.info(
        "Tracked metric: {} with value: {} and properties: {}",
        name,
        value,
        properties
      );
    } catch (Exception e) {
      logger.error(
        "Failed to track metric: {} with properties: {}",
        name,
        properties,
        e
      );
    }
  }
}
