package com.leadersfault.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "faults")
@Data
public class Fault {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String imageUrl;

  private String uploadedBy;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "fault_leaders",
    joinColumns = @JoinColumn(name = "fault_id"),
    inverseJoinColumns = @JoinColumn(name = "leader_id")
  )
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JsonIgnore
  private List<Leader> leaders = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "fault_likes",
    joinColumns = @JoinColumn(name = "fault_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JsonIgnore
  private List<User> likedBy = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "fault_dislikes",
    joinColumns = @JoinColumn(name = "fault_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JsonIgnore
  private List<User> dislikedBy = new ArrayList<>();
}
