package com.hassan.anomaly;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertReviewRepository extends JpaRepository<AlertReview, Long> {

    List<AlertReview> findAllByUsername(String username);

    Optional<AlertReview> findByUsernameAndAccountId(String username, String accountId);

    void deleteByUsernameAndAccountId(String username, String accountId);

    /**
     * Primary-key lookup that goes through JPQL rather than EntityManager.find(),
     * so the Hibernate tenant filter applies. Spring Data's inherited findById
     * delegates to find() and is NOT filtered - see TenantIsolationTest.
     */
    @Query("select r from AlertReview r where r.id = :id")
    Optional<AlertReview> findByIdScoped(@Param("id") Long id);
}