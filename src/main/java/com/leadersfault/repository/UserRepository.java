package com.leadersfault.repository;

import com.leadersfault.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  List<User> findTop5ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
    String username,
    String email
  );
}
