package com.hassan.anomaly.ingest;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAlertRepository extends JpaRepository<JobAlert, Long> {

    List<JobAlert> findAllByJobId(Long jobId, Pageable pageable);

    long countByJobId(Long jobId);

    void deleteByJobId(Long jobId);
}