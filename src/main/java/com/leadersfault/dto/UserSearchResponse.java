package com.leadersfault.dto;

import lombok.Data;

@Data
public class UserSearchResponse {

  private Long id;
  private String username;
  private String email;

  public UserSearchResponse(Long id, String username, String email) {
    this.id = id;
    this.username = username;
    this.email = email;
  }
}
