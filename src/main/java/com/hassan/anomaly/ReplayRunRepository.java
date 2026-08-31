package com.hassan.anomaly;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayRunRepository extends JpaRepository<ReplayRun, Long> {

    List<ReplayRun> findAllByOrderByRunAtDesc();
}