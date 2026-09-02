package com.hassan.anomaly;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplayRunRepository extends JpaRepository<ReplayRun, Long> {

    List<ReplayRun> findAllByUsernameOrderByRunAtDesc(String username);

    Optional<ReplayRun> findByIdAndUsername(Long id, String username);
    
    @Query("select r from ReplayRun r where r.id = :id")
    Optional<ReplayRun> findByIdScoped(@Param("id") Long id);
}
