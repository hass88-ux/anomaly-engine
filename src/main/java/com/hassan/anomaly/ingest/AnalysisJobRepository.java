package com.hassan.anomaly.ingest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findAllByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Optional<AnalysisJob> findByIdAndUsername(Long id, String username);
}