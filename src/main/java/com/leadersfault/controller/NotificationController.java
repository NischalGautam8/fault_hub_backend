package com.leadersfault.controller;

import com.leadersfault.dto.NotificationResponse;
import com.leadersfault.dto.PaginatedResponse;
import com.leadersfault.entity.Notification;
import com.leadersfault.entity.User;
import com.leadersfault.repository.NotificationRepository;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import com.leadersfault.service.UserValidationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserValidationService userValidationService;

  @Autowired
  private JwtUtil jwtUtil;

  @GetMapping
  public ResponseEntity<?> getNotifications(
    HttpServletRequest request,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int limit,
    @RequestParam(required = false) String filter
  ) {
    User user = getUserFromRequest(request);
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    Pageable pageable = PageRequest.of(page, limit);
    Page<Notification> notificationPage;

    if ("unread".equalsIgnoreCase(filter)) {
      notificationPage =
        notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
          user.getId(),
          pageable
        );
    } else if ("read".equalsIgnoreCase(filter)) {
      notificationPage =
        notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(
          user.getId(),
          pageable
        );
    } else {
      notificationPage =
        notificationRepository.findByUserIdOrderByCreatedAtDesc(
          user.getId(),
          pageable
        );
    }

    List<NotificationResponse> notificationResponses = notificationPage
      .stream()
      .map(this::convertToResponse)
      .collect(Collectors.toList());

    PaginatedResponse<NotificationResponse> response = new PaginatedResponse<>(
      notificationResponses,
      notificationPage.getTotalPages()
    );
    return ResponseEntity.ok(response);
  }

  @GetMapping("/unread-count")
  public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
    User user = getUserFromRequest(request);
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(
      user.getId()
    );
    Map<String, Long> response = new HashMap<>();
    response.put("unreadCount", unreadCount);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<?> markAsRead(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    User user = getUserFromRequest(request);
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    Optional<Notification> optionalNotification = notificationRepository.findById(
      id
    );
    if (optionalNotification.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Notification notification = optionalNotification.get();
    if (!notification.getUserId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Forbidden");
    }

    notification.setRead(true);
    notificationRepository.save(notification);
    return ResponseEntity.ok(convertToResponse(notification));
  }

  @PutMapping("/mark-all-read")
  public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
    User user = getUserFromRequest(request);
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    List<Notification> notifications = notificationRepository.findByUserId(
      user.getId()
    );
    notifications.forEach(notification -> notification.setRead(true));
    notificationRepository.saveAll(notifications);

    return ResponseEntity.ok("All notifications marked as read");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteNotification(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    User user = getUserFromRequest(request);
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    Optional<Notification> optionalNotification = notificationRepository.findById(
      id
    );
    if (optionalNotification.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Notification notification = optionalNotification.get();
    if (!notification.getUserId().equals(user.getId())) {
      return ResponseEntity.status(403).body("Forbidden");
    }

    notificationRepository.delete(notification);
    return ResponseEntity.ok("Notification deleted successfully");
  }

  private User getUserFromRequest(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim();
      if (userValidationService.isValidToken(token)) {
        String username = userValidationService.getUsernameFromToken(token);
        return userRepository.findByUsername(username).orElse(null);
      }
    }
    return null;
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
