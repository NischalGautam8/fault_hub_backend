package com.leadersfault.service;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import java.net.BindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SocketIOServerManager implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(
    SocketIOServerManager.class
  );

  @Autowired
  private SocketIOServer server;

  @Override
  public void run(String... args) throws Exception {
    try {
      server.start();
      logger.info(
        "🚀 Socket.IO server started successfully on port: {}",
        server.getConfiguration().getPort()
      );
    } catch (Exception e) {
      // Netty may wrap a BindException inside a RuntimeException; walk the cause chain to detect it.
      Throwable t = e;
      while (t != null) {
        if (t instanceof java.net.BindException) {
          logger.error(
            "⚠️ Socket.IO server failed to start on port {}: address already in use. Socket.IO features will be disabled.",
            server.getConfiguration().getPort(),
            t
          );
          return; // don't rethrow — allow Spring app to continue without Socket.IO
        }
        t = t.getCause();
      }
      // Not a BindException — rethrow to fail startup as before
      throw e;
    }
  }

  @PreDestroy
  public void stopServer() {
    if (server != null) {
      try {
        server.stop();
        logger.info("🛑 Socket.IO server stopped");
      } catch (Exception e) {
        // Some internal Netty resources may not have been initialized if start() failed;
        // swallow the exception and log a warning to avoid crashing shutdown hooks.
        logger.warn(
          "Error while stopping Socket.IO server (ignored): {}",
          e.toString()
        );
      }
    }
  }
}
