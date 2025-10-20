package com.leadersfault.controller;

import com.leadersfault.dto.FaultRequest;
import com.leadersfault.dto.FaultResponse;
import com.leadersfault.dto.LeaderRequest;
import com.leadersfault.dto.LeaderResponse;
import com.leadersfault.dto.PaginatedResponse;
import com.leadersfault.entity.Fault;
import com.leadersfault.entity.Leader;
import com.leadersfault.entity.User;
import com.leadersfault.repository.FaultRepository;
import com.leadersfault.repository.LeaderRepository;
import com.leadersfault.repository.UserRepository;
import com.leadersfault.security.JwtUtil;
import com.leadersfault.service.CloudinaryService;
import com.leadersfault.service.UserValidationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/leaders")
public class LeaderController {

  @Autowired
  private LeaderRepository leaderRepository;

  @Autowired
  private FaultRepository faultRepository;

  @Autowired
  private UserValidationService userValidationService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CloudinaryService cloudinaryService;

  @Autowired
  private JwtUtil jwtUtil;

  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<PaginatedResponse<LeaderResponse>> getAllLeaders(
    HttpServletRequest request,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int limit
  ) {
    Pageable pageable = PageRequest.of(page, limit);
    Page<Leader> leaderPage = leaderRepository.findAll(pageable);

    // If token is invalid or not provided, userId will be null
    // If token is valid, userId will be the ID of the user
    Long userId = isValidToken(request) ? getUserIdFromToken(request) : null;

    List<LeaderResponse> leaderResponses = leaderPage
      .stream()
      .map(leader -> LeaderResponse.fromLeader(leader, userId))
      .collect(Collectors.toList());

    PaginatedResponse<LeaderResponse> response = new PaginatedResponse<>(
      leaderResponses,
      leaderPage.getTotalPages()
    );
    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<?> createLeader(
    HttpServletRequest request,
    @RequestPart("name") String name,
    @RequestPart("description") String description,
    @RequestPart("image") MultipartFile imageFile
  ) {
    try {
      String token = request.getHeader("Authorization");
      if (token == null || !token.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("Unauthorized");
      }
      token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
      jwtUtil.validateJwt(token);
      // Upload image to Cloudinary
      String imageUrl = cloudinaryService.uploadFile(imageFile);

      // Create leader entity and set image URL
      Leader leader = new Leader();
      leader.setName(name);
      leader.setDescription(description);
      leader.setImageUrl(imageUrl);

      return ResponseEntity.ok(leaderRepository.save(leader));
    } catch (Exception e) {
      return ResponseEntity
        .status(500)
        .body("Error uploading image: " + e.getMessage());
    }
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<LeaderResponse> getLeaderById(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    Optional<Leader> optionalLeader = leaderRepository.findById(id);

    if (optionalLeader.isPresent()) {
      Leader leader = optionalLeader.get();

      // If token is invalid or not provided, userId will be null
      // If token is valid, userId will be the ID of the user
      Long userId = isValidToken(request) ? getUserIdFromToken(request) : null;

      LeaderResponse leaderResponse = LeaderResponse.fromLeader(leader, userId);
      return ResponseEntity.ok(leaderResponse);
    }

    return ResponseEntity.notFound().build();
  }

  @GetMapping("/{id}/faults")
  @Transactional(readOnly = true)
  public ResponseEntity<PaginatedResponse<FaultResponse>> getFaultsByLeader(
    HttpServletRequest request,
    @PathVariable Long id,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int limit
  ) {
    User user = getUserFromRequest(request);
    Optional<Leader> optionalLeader = leaderRepository.findById(id);
    if (optionalLeader.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Pageable pageable = PageRequest.of(page, limit);
    Page<Fault> faultPage = faultRepository.findByLeadersContaining(
      optionalLeader.get(),
      pageable
    );
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

  @PostMapping("/{id}/like")
  public ResponseEntity<LeaderResponse> likeLeader(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
      return ResponseEntity.status(401).build();
    }
    token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
    jwtUtil.validateJwt(token);
    Long userId = getUserIdFromToken(request);
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    Optional<Leader> optionalLeader = leaderRepository.findById(id);
    if (optionalLeader.isPresent()) {
      Leader leader = optionalLeader.get();

      // Check if user has already liked this leader
      if (leader.getLikedByUsers().contains(userId)) {
        return ResponseEntity.badRequest().build();
      }

      // If user has previously disliked this leader, remove from dislikes
      if (leader.getDislikedByUsers().contains(userId)) {
        leader.setDislikes(leader.getDislikes() - 1);
        leader.getDislikedByUsers().remove(userId);
      }

      // Add user to likes and increment count
      leader.getLikedByUsers().add(userId);
      leader.setLikes(leader.getLikes() + 1);

      Leader updatedLeader = leaderRepository.save(leader);
      return ResponseEntity.ok(
        LeaderResponse.fromLeader(updatedLeader, userId)
      );
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/dislike")
  public ResponseEntity<LeaderResponse> dislikeLeader(
    HttpServletRequest request,
    @PathVariable Long id
  ) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
      return ResponseEntity.status(401).build();
    }
    token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
    jwtUtil.validateJwt(token);
    Long userId = getUserIdFromToken(request);
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    Optional<Leader> optionalLeader = leaderRepository.findById(id);
    if (optionalLeader.isPresent()) {
      Leader leader = optionalLeader.get();

      // Check if user has already disliked this leader
      if (leader.getDislikedByUsers().contains(userId)) {
        return ResponseEntity.badRequest().build();
      }

      // If user has previously liked this leader, remove from likes
      if (leader.getLikedByUsers().contains(userId)) {
        leader.setLikes(leader.getLikes() - 1);
        leader.getLikedByUsers().remove(userId);
      }

      // Add user to dislikes and increment count
      leader.getDislikedByUsers().add(userId);
      leader.setDislikes(leader.getDislikes() + 1);

      Leader updatedLeader = leaderRepository.save(leader);
      return ResponseEntity.ok(
        LeaderResponse.fromLeader(updatedLeader, userId)
      );
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/faults")
  public ResponseEntity<?> addFaultToLeader(
    HttpServletRequest request,
    @PathVariable Long id,
    @RequestBody Fault fault
  ) {
    if (!isValidToken(request)) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    Optional<Leader> optionalLeader = leaderRepository.findById(id);
    if (optionalLeader.isPresent()) {
      Leader leader = optionalLeader.get();
      fault.getLeaders().add(leader);
      leader.getFaults().add(fault);
      leader.setNumberOfFaults(leader.getNumberOfFaults() + 1);
      leaderRepository.save(leader);
      return ResponseEntity.ok(faultRepository.save(fault));
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/faults/upload")
  public ResponseEntity<?> addFaultToLeaderWithImage(
    HttpServletRequest request,
    @PathVariable Long id,
    @RequestPart("fault") FaultRequest faultRequest,
    @RequestPart("image") MultipartFile imageFile
  ) {
    if (!isValidToken(request)) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    try {
      Optional<Leader> optionalLeader = leaderRepository.findById(id);
      if (optionalLeader.isPresent()) {
        Leader leader = optionalLeader.get();

        // Upload image to Cloudinary
        String imageUrl = cloudinaryService.uploadFile(imageFile);

        Fault fault = new Fault();
        fault.setTitle(faultRequest.getTitle());
        fault.setDescription(faultRequest.getDescription());
        fault.setImageUrl(imageUrl);

        fault.getLeaders().add(leader);
        leader.getFaults().add(fault);
        leader.setNumberOfFaults(leader.getNumberOfFaults() + 1);
        leaderRepository.save(leader);
        return ResponseEntity.ok(faultRepository.save(fault));
      }
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      return ResponseEntity
        .status(500)
        .body("Error uploading image: " + e.getMessage());
    }
  }

  ///search leaders by string
  //search?query=string
  @GetMapping("/search")
  public ResponseEntity<List<LeaderResponse>> searchLeaders(
    HttpServletRequest request,
    @RequestParam String query
  ) {
    List<Leader> leaders = leaderRepository.findTop5ByNameContainingIgnoreCase(
      query
    );

    Long userId = isValidToken(request) ? getUserIdFromToken(request) : null;

    List<LeaderResponse> leaderResponses = leaders
      .stream()
      .map(leader -> LeaderResponse.fromLeader(leader, userId))
      .collect(Collectors.toList());

    return ResponseEntity.ok(leaderResponses);
  }

  private Long getUserIdFromToken(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
      String username = userValidationService.getUsernameFromToken(token);
      if (username != null) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
          return userOptional.get().getId();
        }
      }
    }
    return null;
  }

  private boolean isValidToken(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace
      return userValidationService.isValidToken(token);
    }
    return false;
  }

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
