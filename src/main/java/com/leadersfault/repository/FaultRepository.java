package com.leadersfault.repository;

import com.leadersfault.entity.Fault;
import com.leadersfault.entity.Leader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaultRepository extends JpaRepository<Fault, Long> {
  Page<Fault> findAll(Pageable pageable);
  Page<Fault> findByLeadersContaining(Leader leader, Pageable pageable);
}
