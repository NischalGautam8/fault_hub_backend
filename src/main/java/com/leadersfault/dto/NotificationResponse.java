package com.leadersfault.dto;

import com.leadersfault.entity.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private Long id;
  private String message;
  private NotificationType type;
  private Long faultId;
  private String faultTitle;
  private String actionBy;
  private boolean isRead;
  private LocalDateTime createdAt;
}
