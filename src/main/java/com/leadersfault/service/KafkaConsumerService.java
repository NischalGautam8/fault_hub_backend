package com.leadersfault.service;

import com.leadersfault.dto.NotificationEvent;
import com.leadersfault.dto.NotificationResponse;
import com.leadersfault.entity.Notification;
import com.leadersfault.entity.NotificationType;
import com.leadersfault.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

  private static final Logger logger = LoggerFactory.getLogger(
    KafkaConsumerService.class
  );

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private WebSocketNotificationService webSocketNotificationService;

  @KafkaListener(
    topics = "fault-notifications",
    groupId = "notification-consumer-group"
  )
  public void consumeNotification(NotificationEvent event) {
    logger.info(
      "📥 Received notification event from Kafka - Type: {}, FaultId: {}, Recipient: {}",
      event.getNotificationType(),
      event.getFaultId(),
      event.getFaultOwner()
    );

    // Create notification message based on type
    String message = createMessage(event);

    // Save notification to database
    Notification notification = new Notification();
    notification.setUserId(event.getFaultOwnerId());
    notification.setMessage(message);
    notification.setType(event.getNotificationType());
    notification.setFaultId(event.getFaultId());
    notification.setFaultTitle(event.getFaultTitle());
    notification.setActionBy(event.getActionBy());
    notification.setRead(false);

    Notification savedNotification = notificationRepository.save(notification);
    logger.info(
      "💾 Notification saved to database - ID: {}, UserId: {}, Message: '{}'",
      savedNotification.getId(),
      savedNotification.getUserId(),
      message
    );

    // Send real-time notification via Socket.IO
    NotificationResponse response = convertToResponse(savedNotification);
    webSocketNotificationService.sendNotificationToUser(
      event.getFaultOwnerId(),
      response
    );
  }

  private String createMessage(NotificationEvent event) {
    if (event.getNotificationType() == NotificationType.FAULT_LIKED) {
      return event.getActionBy() + " agreed with the fault you posted";
    } else {
      return event.getActionBy() + " disagreed with the fault you posted";
    }
  }

  private NotificationResponse convertToResponse(Notification notification) {
    NotificationResponse response = new NotificationResponse();
    response.setId(notification.getId());
    response.setMessage(notification.getMessage());
    response.setType(notification.getType());
    response.setFaultId(notification.getFaultId());
    response.setFaultTitle(notification.getFaultTitle());
    response.setActionBy(notification.getActionBy());
    response.setRead(notification.isRead());
    response.setCreatedAt(notification.getCreatedAt());
    return response;
  }
}
