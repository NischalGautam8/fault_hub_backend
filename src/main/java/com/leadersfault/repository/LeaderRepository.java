package com.leadersfault.repository;

import com.leadersfault.entity.Leader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderRepository extends JpaRepository<Leader, Long> {
  java.util.List<Leader> findTop5ByNameContainingIgnoreCase(String name);
}
