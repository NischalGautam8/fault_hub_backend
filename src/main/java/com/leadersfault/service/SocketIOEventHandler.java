package com.leadersfault.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SocketIOEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(
    SocketIOEventHandler.class
  );

  @Autowired
  private SocketIOServer server;

  @PostConstruct
  public void init() {
    server.addConnectListener(onConnect());
    server.addDisconnectListener(onDisconnect());

    // Listen for join-room event
    server.addEventListener(
      "join-room",
      String.class,
      (client, userId, ackSender) -> {
        String roomName = "user:" + userId;
        client.joinRoom(roomName);
        logger.info(
          "✅ Client {} joined room: {}",
          client.getSessionId(),
          roomName
        );

        // Send acknowledgment
        if (ackSender != null) {
          ackSender.sendAckData("joined", roomName);
        }
      }
    );

    // Listen for leave-room event
    server.addEventListener(
      "leave-room",
      String.class,
      (client, userId, ackSender) -> {
        String roomName = "user:" + userId;
        client.leaveRoom(roomName);
        logger.info(
          "👋 Client {} left room: {}",
          client.getSessionId(),
          roomName
        );

        // Send acknowledgment
        if (ackSender != null) {
          ackSender.sendAckData("left", roomName);
        }
      }
    );
  }

  private ConnectListener onConnect() {
    return client -> {
      String clientId = client.getSessionId().toString();
      logger.info(
        "🔌 Socket.IO client connected - Session ID: {}, Remote Address: {}",
        clientId,
        client.getRemoteAddress()
      );
    };
  }

  private DisconnectListener onDisconnect() {
    return client -> {
      String clientId = client.getSessionId().toString();
      logger.info(
        "🔌 Socket.IO client disconnected - Session ID: {}",
        clientId
      );
    };
  }
}
