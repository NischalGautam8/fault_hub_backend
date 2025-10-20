package com.leadersfault.repository;

import com.leadersfault.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository
  extends JpaRepository<Notification, Long> {
  Page<Notification> findByUserIdOrderByCreatedAtDesc(
    Long userId,
    Pageable pageable
  );

  Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
    Long userId,
    Pageable pageable
  );

  Page<Notification> findByUserIdAndIsReadTrueOrderByCreatedAtDesc(
    Long userId,
    Pageable pageable
  );

  List<Notification> findByUserId(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);
}
