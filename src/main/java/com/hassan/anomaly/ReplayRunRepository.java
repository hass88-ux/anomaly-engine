package com.hassan.anomaly;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayRunRepository extends JpaRepository<ReplayRun, Long> {

    List<ReplayRun> findAllByUsernameOrderByRunAtDesc(String username);

    Optional<ReplayRun> findByIdAndUsername(Long id, String username);
}