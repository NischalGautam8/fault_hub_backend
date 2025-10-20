package com.leadersfault.dto;

import com.leadersfault.entity.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

  private NotificationType notificationType;
  private Long faultId;
  private String faultTitle;
  private String faultOwner;
  private Long faultOwnerId;
  private String actionBy;
  private LocalDateTime timestamp;
}
