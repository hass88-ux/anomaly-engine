package com.hassan.anomaly.ingest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findAllByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Optional<AnalysisJob> findByIdAndUsername(Long id, String username);

    /**
     * Primary-key lookup that goes through JPQL rather than EntityManager.find(),
     * so the Hibernate tenant filter applies. Spring Data's inherited findById
     * delegates to find() and is NOT filtered - see TenantIsolationTest.
     */
    @Query("select j from AnalysisJob j where j.id = :id")
    Optional<AnalysisJob> findByIdScoped(@Param("id") Long id);
}