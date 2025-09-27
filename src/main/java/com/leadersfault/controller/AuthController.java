package com.leadersfault.controller;

import com.leadersfault.config.TelemetryLogger;
import com.leadersfault.dto.AuthRequest;
import com.leadersfault.dto.AuthResponse;
import com.leadersfault.dto.RegisterRequest;
import com.leadersfault.entity.User;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
    // Track login attempt
    telemetryLogger.trackEvent(
      "Login attempt",
      Map.of("username", authRequest.getUsername())
    );

    // Find user by username
    var userOptional = userRepository.findByUsername(authRequest.getUsername());

    // Check if user exists and password matches
    if (userOptional.isPresent()) {
      User user = userOptional.get();
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
  }

  @PostMapping("/validate")
  public ResponseEntity<?> validateToken(
    @RequestHeader("Authorization") String token
  ) {
    try {
      jwtUtil.validateJwt(token);
      return ResponseEntity.ok("Token is valid");
    } catch (Exception e) {
      return ResponseEntity.status(401).body(e.getMessage());
    }
  }
}
