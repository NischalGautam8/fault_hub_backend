package com.leadersfault.dto;

import java.util.List;
import lombok.Data;

@Data
public class FaultRequest {

  private String title;
  private String description;
  private String imageUrl;
  private List<Long> leaderIds;
  private int page;
  private int limit;
}
