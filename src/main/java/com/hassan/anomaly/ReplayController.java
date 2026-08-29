package com.hassan.anomaly;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @GetMapping("/default")
    public ReplayResult runDefault() {
        return replayService.run(ReplayRequest.defaults());
    }

    @PostMapping
    public ReplayResult run(@RequestBody ReplayRequest request) {
        return replayService.run(request);
    }
}
