package com.hassan.anomaly;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final ReplayService replayService;
    private final ReplayRunRepository repository;

    public ReplayController(ReplayService replayService, ReplayRunRepository repository) {
        this.replayService = replayService;
        this.repository = repository;
    }

    @GetMapping("/default")
    public ReplayResult runDefault(@AuthenticationPrincipal String username) {
        return replayService.run(ReplayRequest.defaults(), username);
    }

    @PostMapping
    public ReplayResult run(@Valid @RequestBody ReplayRequest request,
                            @AuthenticationPrincipal String username) {
        return replayService.run(request, username);
    }

    @GetMapping("/history")
    public List<ReplayRun> history(@AuthenticationPrincipal String username) {
        return repository.findAllByUsernameOrderByRunAtDesc(username);
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ReplayRun> byId(@PathVariable Long id,
                                          @AuthenticationPrincipal String username) {
        return repository.findByIdAndUsername(id, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}