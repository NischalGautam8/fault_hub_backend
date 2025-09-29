package com.leadersfault.controller;

import com.leadersfault.config.TelemetryLogger;
import com.leadersfault.dto.AuthRequest;
import com.leadersfault.dto.AuthResponse;
import com.leadersfault.dto.RegisterRequest;
import com.leadersfault.dto.UserSearchResponse;
import com.leadersfault.entity.User;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private TelemetryLogger telemetryLogger;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @PostMapping("/register")
  public ResponseEntity<?> registerUser(
    @RequestBody RegisterRequest registerRequest
  ) {
    if (
      userRepository.findByUsername(registerRequest.getUsername()).isPresent()
    ) {
      return ResponseEntity.badRequest().body("Username is already taken!");
    }

    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setEmail(registerRequest.getEmail());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

    userRepository.save(user);

    return ResponseEntity.ok("User registered successfully!");
  }

  @PostMapping("/login")
  public ResponseEntity<?> createAuthenticationToken(
    @RequestBody AuthRequest authRequest
  ) {
    if (
      authRequest == null ||
      authRequest.getUsername() == null ||
      authRequest.getPassword() == null
    ) {
      telemetryLogger.trackEvent(
        "Login attempt - Invalid Request",
        Map.of("reason", "null authRequest or fields")
      );
      return ResponseEntity
        .badRequest()
        .body("Username and password must be provided.");
    }

    // Track login attempt
    telemetryLogger.trackEvent(
      "Login attempt",
      Map.of("username", authRequest.getUsername())
    );

    try {
      // Find user by username
      var userOptional = userRepository.findByUsername(
        authRequest.getUsername()
      );

      // Check if user exists and password matches
      if (userOptional.isPresent()) {
        User user = userOptional.get();
        if (user.getPassword() == null) {
          telemetryLogger.trackEvent(
            "Failed login - User password null",
            Map.of("username", authRequest.getUsername())
          );
          return ResponseEntity
            .status(500)
            .body("User data corrupted: password not set.");
        }
        if (
          passwordEncoder.matches(authRequest.getPassword(), user.getPassword())
        ) {
          // Track successful login
          telemetryLogger.trackEvent(
            "Successful login",
            Map.of("username", authRequest.getUsername())
          );

          // Generate JWT token
          String token = jwtUtil.generateToken(user.getUsername());
          return ResponseEntity.ok(new AuthResponse(token));
        }
      }

      // Track failed login
      telemetryLogger.trackEvent(
        "Failed login",
        Map.of("username", authRequest.getUsername())
      );

      return ResponseEntity.status(401).body("Incorrect username or password");
    } catch (Exception e) {
      telemetryLogger.trackException(e);
      return ResponseEntity
        .status(500)
        .body("An unexpected error occurred during login: " + e.getMessage());
    }
  }

  @PostMapping("/validate")
  public ResponseEntity<?> validateToken(
    @RequestHeader("Authorization") String authHeader
  ) {
    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("Invalid Authorization header format");
      }
      String token = authHeader.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
      jwtUtil.validateJwt(token);
      return ResponseEntity.ok("Token is valid");
    } catch (Exception e) {
      return ResponseEntity.status(401).body(e.getMessage());
    }
  }

  @GetMapping("/search")
  public ResponseEntity<?> searchUsers(
    @RequestParam String query,
    @RequestHeader("Authorization") String authHeader
  ) {
    try {
      // Extract token from Authorization header
      String token = authHeader.replace("Bearer ", "").trim(); // Also trim whitespace
      jwtUtil.validateJwt(token);

      // Validate query parameter
      if (query == null || query.trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Query parameter is required");
      }

      // Search users (max 5 results) - search in both username and email
      String searchQuery = query.trim();
      List<User> users = userRepository.findTop5ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        searchQuery,
        searchQuery
      );

      // Convert to DTO to avoid exposing sensitive information
      List<UserSearchResponse> searchResults = users
        .stream()
        .map(user ->
          new UserSearchResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail()
          )
        )
        .collect(Collectors.toList());

      telemetryLogger.trackEvent(
        "User search performed",
        Map.of(
          "query",
          query,
          "resultsCount",
          String.valueOf(searchResults.size())
        )
      );

      return ResponseEntity.ok(searchResults);
    } catch (Exception e) {
      telemetryLogger.trackException(e);
      return ResponseEntity.status(401).body("Unauthorized: " + e.getMessage());
    }
  }
}
