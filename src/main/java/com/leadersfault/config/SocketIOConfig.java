package com.leadersfault.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SocketIOConfig {

  @Value("${socketio.host:localhost}")
  private String host;

  @Value("${socketio.port:9093}")
  private Integer port;

  @Bean
  public SocketIOServer socketIOServer() {
    Configuration config = new Configuration();
    config.setHostname(host);
    config.setPort(port);

    // Create JacksonJsonSupport and directly pass the JavaTimeModule to its constructor
    JacksonJsonSupport jsonSupport = new JacksonJsonSupport(
      new JavaTimeModule()
    );
    config.setJsonSupport(jsonSupport);

    // CORS configuration - allow all origins (adjust for production)
    config.setOrigin("*");

    // WebSocket transport configuration
    config.setAllowCustomRequests(true);
    config.setUpgradeTimeout(10000);
    config.setPingTimeout(60000);
    config.setPingInterval(25000);

    return new SocketIOServer(config);
  }
}
