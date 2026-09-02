package com.hassan.anomaly;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/reviews")
public class AlertReviewController {

    private final AlertReviewRepository repository;

    public AlertReviewController(AlertReviewRepository repository) {
        this.repository = repository;
    }

    public record ReviewRequest(
            @NotNull(message = "status is required") ReviewStatus status,
            @Size(max = 500, message = "note must be 500 characters or fewer") String note) {
    }

    @GetMapping
    public List<AlertReview> all(@AuthenticationPrincipal String username) {
        return repository.findAllByUsername(username);
    }

    @PutMapping("/{accountId}")
    @Transactional
    public ResponseEntity<AlertReview> set(
            @PathVariable String accountId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal String username) {

        if (request.status() == ReviewStatus.NEW) {
            repository.deleteByUsernameAndAccountId(username, accountId);
            return ResponseEntity.noContent().build();
        }

        AlertReview review = repository
                .findByUsernameAndAccountId(username, accountId)
                .orElseGet(() -> new AlertReview(username, accountId, request.status(), request.note()));

        review.update(request.status(), request.note());

        return ResponseEntity.ok(repository.save(review));
    }

    @DeleteMapping("/{accountId}")
    @Transactional
    public ResponseEntity<Void> clear(
            @PathVariable String accountId,
            @AuthenticationPrincipal String username) {
        repository.deleteByUsernameAndAccountId(username, accountId);
        return ResponseEntity.noContent().build();
    }
}