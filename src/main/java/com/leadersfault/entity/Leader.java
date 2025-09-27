package com.leadersfault.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Entity
@Table(name = "leaders")
@Data
public class Leader {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String imageUrl;

  @Column(nullable = false, columnDefinition = "integer default 0")
  private int likes;

  @Column(nullable = false, columnDefinition = "integer default 0")
  private int dislikes;

  @Column(
    name = "number_of_faults",
    nullable = false,
    columnDefinition = "integer default 0"
  )
  private int numberOfFaults;

  @ManyToMany(mappedBy = "leaders", fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Fault> faults = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
    name = "leader_likes",
    joinColumns = @JoinColumn(name = "leader_id")
  )
  @Column(name = "user_id")
  private Set<Long> likedByUsers = new HashSet<>();

  @ElementCollection
  @CollectionTable(
    name = "leader_dislikes",
    joinColumns = @JoinColumn(name = "leader_id")
  )
  @Column(name = "user_id")
  private Set<Long> dislikedByUsers = new HashSet<>();
}
