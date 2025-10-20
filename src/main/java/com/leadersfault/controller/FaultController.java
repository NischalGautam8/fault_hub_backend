package com.leadersfault.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadersfault.dto.FaultRequest;
import com.leadersfault.dto.FaultResponse;
import com.leadersfault.dto.LeaderResponse;
import com.leadersfault.dto.NotificationEvent;
import com.leadersfault.dto.PaginatedResponse;
import com.leadersfault.entity.Fault;
import com.leadersfault.entity.Leader;
import com.leadersfault.entity.NotificationType;
import com.leadersfault.entity.User;
import com.leadersfault.repository.FaultRepository;
import com.leadersfault.repository.LeaderRepository;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import com.leadersfault.service.CloudinaryService;
import com.leadersfault.service.KafkaProducerService;
import com.leadersfault.service.UserValidationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/faults")
public class FaultController {

  private static final Logger logger = LoggerFactory.getLogger(
    FaultController.class
  );

  @Autowired
  private FaultRepository faultRepository;

  @Autowired
  private LeaderRepository leaderRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserValidationService userValidationService;

  @Autowired
  private CloudinaryService cloudinaryService;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private KafkaProducerService kafkaProducerService;

  @PostMapping
  public ResponseEntity<?> createFault(
    HttpServletRequest request,
    @RequestPart("title") String title,
    @RequestPart("description") String description,
    @RequestPart("leaderIds") String leaderIdsJson,
    @RequestPart("image") MultipartFile imageFile
  ) {
    try {
      String token = request.getHeader("Authorization");
      if (token == null || !token.startsWith("Bearer ")) {
        return ResponseEntity
          .status(401)
          .body("Unauthorized: Missing or invalid token");
      }
      token = token.substring(7).trim(); // Remove "Bearer " prefix and trim any whitespace
      jwtUtil.validateJwt(token);
      // Upload image to Cloudinary
      String imageUrl = cloudinaryService.uploadFile(imageFile);

      Fault fault = new Fault();
      fault.setTitle(title);
      fault.setDescription(description);
      fault.setImageUrl(imageUrl);

      String username = userValidationService.getUsernameFromToken(token);
      fault.setUploadedBy(username);

      // Deserialize leaderIds from JSON string
      ObjectMapper objectMapper = new ObjectMapper();
      List<Long> leaderIds = objectMapper.readValue(
        leaderIdsJson,
        objectMapper
          .getTypeFactory()
          .constructCollectionType(List.class, Long.class)
      );

      // Associate fault with leaders
      if (leaderIds != null && !leaderIds.isEmpty()) {
        List<Leader> leaders = leaderIds
          .stream()
          .map(leaderRepository::findById)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toList());

        fault.setLeaders(leaders);
        // Also update the leaders to include this fault and increment the number of faults
        for (Leader leader : leaders) {
          leader.setNumberOfFaults(leader.getNumberOfFaults() + 1);
        }
        leaderRepository.saveAll(leaders);
      }

      return ResponseEntity.ok(faultRepository.save(fault));
    } catch (JsonProcessingException e) {
      return ResponseEntity
        .status(400)
        .body("Invalid format for leaderIds: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity
        .status(500)
        .body("Error uploading image: " + e.getMessage());
    }
  }

  // @PutMapping("/{id}")
  // public ResponseEntity<?> updateFault(
  //   HttpServletRequest request,
  //   @PathVariable Long id,
  //   @RequestBody FaultRequest faultRequest
  // ) {
  //   if (!isValidToken(request)) {
  //     return ResponseEntity.status(401).body("Unauthorized");
  //   }

  //   Optional<Fault> optionalFault = faultRepository.findById(id);
  //   if (optionalFault.isPresent()) {
  //     Fault fault = optionalFault.get();
  //     fault.setTitle(faultRequest.getTitle());
  //     fault.setDescription(faultRequest.getDescription());
  //     fault.setImageUrl(faultRequest.getImageUrl());

  //     // Update leader associations if provided
  //     if (faultRequest.getLeaderIds() != null) {
  //       List<Leader> oldLeaders = new java.util.ArrayList<>(fault.getLeaders());
  //       List<Long> newLeaderIds = faultRequest.getLeaderIds();

  //       // Leaders to be removed
  //       List<Leader> leadersToRemove = oldLeaders
  //         .stream()
  //         .filter(leader -> !newLeaderIds.contains(leader.getId()))
  //         .collect(Collectors.toList());
  //       for (Leader leader : leadersToRemove) {
  //         leader.setNumberOfFaults(Math.max(0, leader.getNumberOfFaults() - 1));
  //       }
  //       if (!leadersToRemove.isEmpty()) {
  //         leaderRepository.saveAll(leadersToRemove);
  //       }

  //       // Leaders to be added
  //       List<Leader> newLeaders = newLeaderIds
  //         .stream()
  //         .map(leaderRepository::findById)
  //         .filter(Optional::isPresent)
  //         .map(Optional::get)
  //         .collect(Collectors.toList());

  //       List<Leader> leadersToAdd = newLeaders
  //         .stream()
  //         .filter(leader -> !oldLeaders.contains(leader))
  //         .collect(Collectors.toList());
  //       for (Leader leader : leadersToAdd) {
  //         leader.setNumberOfFaults(leader.getNumberOfFaults() + 1);
  //       }
  //       if (!leadersToAdd.isEmpty()) {
  //         leaderRepository.saveAll(leadersToAdd);
  //       }

  //       fault.setLeaders(newLeaders);
  //     }

  //     return ResponseEntity.ok(faultRepository.save(fault));
  //   }
  //   return ResponseEntity.notFound().build();
  // }

  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<PaginatedResponse<FaultResponse>> getFaults(
    HttpServletRequest request,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int limit
  ) {
    User user = getUserFromRequest(request);
    Pageable pageable = PageRequest.of(page, limit);
    Page<Fault> faultPage = faultRepository.findAll(pageable);

    List<FaultResponse> faultResponses = faultPage
      .stream()
      .map(fault -> convertToDto(fault, user, faultPage.getTotalPages()))
      .collect(Collectors.toList());

    PaginatedResponse<FaultResponse> response = new PaginatedResponse<>(
      faultResponses,
      faultPage.getTotalPages()
    );
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<FaultResponse> getFault(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    User user = getUserFromRequest(request);
    return faultRepository
      .findById(id)
      .map(fault -> convertToDto(fault, user, 1)) // For single fault, totalPages is 1
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteFault(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
      return ResponseEntity
        .status(401)
        .body("Unauthorized: Missing or invalid token");
    }
    token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
    jwtUtil.validateJwt(token);

    String username = userValidationService.getUsernameFromToken(token);

    Optional<Fault> optionalFault = faultRepository.findById(id);
    if (optionalFault.isPresent()) {
      Fault fault = optionalFault.get();

      if (!fault.getUploadedBy().equals(username)) {
        return ResponseEntity
          .status(403)
          .body("Forbidden: You are not the creator of this fault");
      }

      // Decrement the number of faults for each associated leader
      List<Leader> leaders = fault.getLeaders();
      if (leaders != null && !leaders.isEmpty()) {
        for (Leader leader : leaders) {
          leader.setNumberOfFaults(Math.max(0, leader.getNumberOfFaults() - 1));
        }
        leaderRepository.saveAll(leaders);
      }

      faultRepository.delete(fault);
      return ResponseEntity.ok().body("Fault deleted successfully");
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/like")
  public ResponseEntity<?> likeFault(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    return handleLikeDislike(request, id, true);
  }

  @PostMapping("/{id}/dislike")
  public ResponseEntity<?> dislikeFault(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    return handleLikeDislike(request, id, false);
  }

  private ResponseEntity<?> handleLikeDislike(
    HttpServletRequest request,
    Long id,
    boolean isLike
  ) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
      return ResponseEntity.status(401).body("Unauthorized");
    }
    token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
    jwtUtil.validateJwt(token);

    String username = userValidationService.getUsernameFromToken(token);
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isEmpty()) {
      return ResponseEntity.status(401).body("User not found");
    }
    User user = userOptional.get();

    Optional<Fault> optionalFault = faultRepository.findById(id);
    if (optionalFault.isPresent()) {
      Fault fault = optionalFault.get();
      List<User> likedBy = fault.getLikedBy();
      List<User> dislikedBy = fault.getDislikedBy();

      boolean shouldNotify = false;
      NotificationType notificationType = null;

      if (isLike) {
        if (likedBy.contains(user)) {
          likedBy.remove(user); // Unlike if already liked
        } else {
          likedBy.add(user);
          dislikedBy.remove(user); // Remove from dislikes if present
          shouldNotify = true;
          notificationType = NotificationType.FAULT_LIKED;
        }
      } else {
        if (dislikedBy.contains(user)) {
          dislikedBy.remove(user); // Undislike if already disliked
        } else {
          dislikedBy.add(user);
          likedBy.remove(user); // Remove from likes if present
          shouldNotify = true;
          notificationType = NotificationType.FAULT_DISLIKED;
        }
      }

      faultRepository.save(fault);

      // Send notification only if user is not the fault owner (no self-notifications)
      if (shouldNotify && !fault.getUploadedBy().equals(username)) {
        logger.info(
          "🚀 Notification triggered - User '{}' {} fault '{}' (ID: {}) owned by '{}'",
          username,
          isLike ? "liked" : "disliked",
          fault.getTitle(),
          fault.getId(),
          fault.getUploadedBy()
        );

        // Get fault owner user ID
        Optional<User> faultOwnerOptional = userRepository.findByUsername(
          fault.getUploadedBy()
        );
        if (faultOwnerOptional.isPresent()) {
          User faultOwner = faultOwnerOptional.get();

          NotificationEvent event = new NotificationEvent(
            notificationType,
            fault.getId(),
            fault.getTitle(),
            fault.getUploadedBy(),
            faultOwner.getId(),
            username,
            LocalDateTime.now()
          );

          try {
            kafkaProducerService.sendNotification(event);
          } catch (Exception ex) {
            logger.error(
              "❌ Failed to send notification event (ignored): {}",
              ex.toString()
            );
          }
        } else {
          logger.warn(
            "⚠️ Fault owner '{}' not found in database, notification not sent",
            fault.getUploadedBy()
          );
        }
      } else if (shouldNotify) {
        logger.debug(
          "🔇 Skipping self-notification - User '{}' {} their own fault",
          username,
          isLike ? "liked" : "disliked"
        );
      }

      return ResponseEntity.ok(convertToDto(fault, user, 1)); // For single fault, totalPages is 1
    }
    return ResponseEntity.notFound().build();
  }

  // private boolean isValidToken(HttpServletRequest request) {
  //   String token = request.getHeader("Authorization");
  //   if (token != null && token.startsWith("Bearer ")) {
  //     token = token.substring(7);
  //     try {
  //       jwtUtil.validateJwt(token);
  //       return true;
  //     } catch (Exception e) {
  //       return false;
  //     }
  //   }
  //   return false;
  // }

  private User getUserFromRequest(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
      if (userValidationService.isValidToken(token)) {
        String username = userValidationService.getUsernameFromToken(token);
        return userRepository.findByUsername(username).orElse(null);
      }
    }
    return null;
  }

  private FaultResponse convertToDto(Fault fault, User user, int totalPages) {
    FaultResponse faultResponse = new FaultResponse();
    faultResponse.setId(fault.getId());
    faultResponse.setTitle(fault.getTitle());
    faultResponse.setDescription(fault.getDescription());
    faultResponse.setImageUrl(fault.getImageUrl());
    faultResponse.setUploadedBy(fault.getUploadedBy());
    faultResponse.setTotalPages(totalPages);

    if (fault.getLeaders() != null) {
      faultResponse.setLeaders(
        fault
          .getLeaders()
          .stream()
          .map(this::convertLeaderToDto)
          .collect(Collectors.toList())
      );
    }

    int likes = fault.getLikedBy().size();
    int dislikes = fault.getDislikedBy().size();
    faultResponse.setLikes(likes);
    faultResponse.setDislikes(dislikes);

    if (likes + dislikes > 0) {
      faultResponse.setPercentageLiked(
        (double) likes / (likes + dislikes) * 100
      );
    } else {
      faultResponse.setPercentageLiked(0);
    }

    if (user != null) {
      if (fault.getLikedBy().contains(user)) {
        faultResponse.setVoteStatus("liked");
      } else if (fault.getDislikedBy().contains(user)) {
        faultResponse.setVoteStatus("disliked");
      } else {
        faultResponse.setVoteStatus("none");
      }
    } else {
      faultResponse.setVoteStatus("none");
    }

    return faultResponse;
  }

  private LeaderResponse convertLeaderToDto(Leader leader) {
    LeaderResponse leaderResponse = new LeaderResponse();
    leaderResponse.setId(leader.getId());
    leaderResponse.setName(leader.getName());
    leaderResponse.setDescription(leader.getDescription());
    leaderResponse.setImageUrl(leader.getImageUrl());
    leaderResponse.setLikes(leader.getLikes());
    leaderResponse.setDislikes(leader.getDislikes());
    leaderResponse.setNumberOfFaults(leader.getNumberOfFaults());
    return leaderResponse;
  }
}
