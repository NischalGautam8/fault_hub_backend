package com.leadersfault.service;

import com.leadersfault.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

  private static final Logger logger = LoggerFactory.getLogger(
    KafkaProducerService.class
  );
  private static final String TOPIC = "fault-notifications";

  @Autowired
  private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

  public void sendNotification(NotificationEvent event) {
    logger.info(
      "📤 Publishing notification event to Kafka - Type: {}, FaultId: {}, Owner: {}, ActionBy: {}",
      event.getNotificationType(),
      event.getFaultId(),
      event.getFaultOwner(),
      event.getActionBy()
    );
    try {
      // kafkaTemplate.send(...) returns a CompletableFuture in this project; use whenComplete
      kafkaTemplate
        .send(TOPIC, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            logger.error(
              "❌ Failed to publish notification event to Kafka: {}",
              ex.toString()
            );
          } else {
            logger.info(
              "✅ Notification event published successfully to topic: {}",
              TOPIC
            );
          }
        });
    } catch (Exception e) {
      // Protect callers from Kafka outages by logging the error and proceeding.
      logger.error(
        "❌ Exception while sending notification to Kafka (will be ignored): {}",
        e.toString()
      );
    }
  }
}
