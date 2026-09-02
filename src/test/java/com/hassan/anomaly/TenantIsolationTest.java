package com.hassan.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AlertReviewRepository reviews;

    @BeforeEach
    void seedTwoTenants() {
        reviews.save(new AlertReview("alice", "acc-1", ReviewStatus.REVIEWED, "alice's note"));
        reviews.save(new AlertReview("alice", "acc-2", ReviewStatus.ESCALATED, null));
        reviews.save(new AlertReview("bob", "acc-9", ReviewStatus.DISMISSED, "bob's note"));
        entityManager.flush();
        entityManager.clear();
    }

    private void actAs(String username) {
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantUsername", username);
    }

    @Test
    void findAllReturnsOnlyTheCurrentTenantsRows() {
        actAs("alice");

        List<AlertReview> all = reviews.findAll();

        assertEquals(2, all.size(), "alice should see exactly her own two reviews");
        assertTrue(all.stream().allMatch(r -> "alice".equals(r.getUsername())),
                "no row belonging to another user should be returned");
    }

    @Test
    void bobCannotSeeAlicesRows() {
        actAs("bob");

        List<AlertReview> all = reviews.findAll();

        assertEquals(1, all.size());
        assertEquals("acc-9", all.get(0).getAccountId());
    }

    @Test
    void findByIdIsNotFilteredAndMustBeScopedExplicitly() {
        actAs("alice");
        Long alicesId = reviews.findAll().stream()
                .findFirst()
                .map(AlertReview::getId)
                .orElseThrow();

        entityManager.clear();
        actAs("bob");

        // Hibernate filters do not apply to EntityManager.find(), which is what
        // Spring Data's findById delegates to. A primary-key load bypasses the
        // filter, so bob CAN reach alice's row this way.
        assertTrue(reviews.findById(alicesId).isPresent(),
                "documents that findById is not filtered - this is why repository "
                        + "methods must still scope by username explicitly");

        // The scoped method is what actually protects the row.
        assertTrue(reviews.findByUsernameAndAccountId("bob", "acc-1").isEmpty(),
                "the explicitly scoped query returns nothing for the wrong tenant");
    }

    @Test
    void withoutAFilterEveryRowIsVisible() {
        // Documents what the filter is protecting against: with no tenant enabled,
        // the same query returns all three rows across both users.
        List<AlertReview> all = reviews.findAll();

        assertEquals(3, all.size(),
                "unfiltered queries see everything - this is why the filter exists");
    }
}