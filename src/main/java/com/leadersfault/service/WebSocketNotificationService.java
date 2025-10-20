package com.leadersfault.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.leadersfault.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

  private static final Logger logger = LoggerFactory.getLogger(
    WebSocketNotificationService.class
  );

  @Autowired
  private SocketIOServer socketIOServer;

  /**
   * Send notification via Socket.IO
   * Clients use socket.on('notification', ...) to receive notifications
   */
  public void sendNotificationToUser(
    Long userId,
    NotificationResponse notification
  ) {
    String eventName = "notification";
    logger.info(
      "🔔 Sending Socket.IO notification - Event: {}, UserId: {}, Type: {}, Message: '{}'",
      eventName,
      userId,
      notification.getType(),
      notification.getMessage()
    );

    // Emit to specific user's room
    socketIOServer
      .getRoomOperations("user:" + userId)
      .sendEvent(eventName, notification);

    logger.info(
      "✅ Socket.IO notification sent successfully to user: {}",
      userId
    );
  }
}
