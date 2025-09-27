package com.leadersfault.service;

import com.leadersfault.entity.User;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserValidationService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public boolean isValidUser(String username, String password) {
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
      User user = userOptional.get();
      return passwordEncoder.matches(password, user.getPassword());
    }
    return false;
  }

  public String generateToken(String username) {
    return jwtUtil.generateToken(username);
  }

  public boolean isValidToken(String token) {
    try {
      String username = jwtUtil.extractUsername(token);
      if (username == null) {
        return false;
      }

      // Validate token directly with username
      return jwtUtil.validateToken(token, username);
    } catch (Exception e) {
      return false;
    }
  }

  public String getUsernameFromToken(String token) {
    try {
      return jwtUtil.extractUsername(token);
    } catch (Exception e) {
      return null;
    }
  }

  public void invalidateToken(String token) {
    // In JWT, tokens are stateless and can't be invalidated on the server side
    // You would need to implement a token blacklist or use a different approach
    // For now, we'll just leave this method empty
  }
}
