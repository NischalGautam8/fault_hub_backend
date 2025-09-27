package com.leadersfault.dto;

import java.util.List;
import lombok.Data;

@Data
public class FaultResponse {

  private Long id;
  private String title;
  private String description;
  private String imageUrl;
  private String uploadedBy;
  private List<LeaderResponse> leaders;
  private int likes;
  private int dislikes;
  private double percentageLiked;
  private String voteStatus;
  private int totalPages;
}
