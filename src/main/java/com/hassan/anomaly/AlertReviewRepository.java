package com.hassan.anomaly;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertReviewRepository extends JpaRepository<AlertReview, Long> {

    List<AlertReview> findAllByUsername(String username);

    Optional<AlertReview> findByUsernameAndAccountId(String username, String accountId);

    void deleteByUsernameAndAccountId(String username, String accountId);
}