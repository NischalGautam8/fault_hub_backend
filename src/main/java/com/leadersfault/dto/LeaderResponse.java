package com.leadersfault.dto;

import com.leadersfault.entity.Leader;
import java.util.Set;
import lombok.Data;

@Data
public class LeaderResponse {

  private Long id;
  private String name;
  private String description;
  private String imageUrl;
  private int likes;
  private int dislikes;
  private int numberOfFaults;
  private String voteStatus; // "LIKED", "DISLIKED", or null
  private int faultCount;

  public static LeaderResponse fromLeader(Leader leader, Long userId) {
    LeaderResponse response = new LeaderResponse();
    response.setId(leader.getId());
    response.setName(leader.getName());
    response.setDescription(leader.getDescription());
    response.setImageUrl(leader.getImageUrl());
    response.setLikes(leader.getLikes());
    response.setDislikes(leader.getDislikes());
    response.setNumberOfFaults(leader.getNumberOfFaults());
    response.setFaultCount(leader.getNumberOfFaults()); // Populate faultCount

    // Set vote status based on user's previous actions
    if (userId != null) {
      if (
        leader.getLikedByUsers() != null &&
        leader.getLikedByUsers().contains(userId)
      ) {
        response.setVoteStatus("LIKED");
      } else if (
        leader.getDislikedByUsers() != null &&
        leader.getDislikedByUsers().contains(userId)
      ) {
        response.setVoteStatus("DISLIKED");
      } else {
        response.setVoteStatus(null);
      }
    } else {
      response.setVoteStatus(null);
    }

    return response;
  }
}
