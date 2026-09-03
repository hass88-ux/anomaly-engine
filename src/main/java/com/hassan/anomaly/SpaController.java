package com.hassan.anomaly;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React Router owns the client-side routes. A browser requesting /alerts directly —
 * on a refresh or a pasted link — asks the server for a path Spring has no mapping
 * for. Forwarding those to index.html lets React take over routing once loaded.
 *
 * Paths under /api are excluded, so a genuinely missing endpoint still 404s rather
 * than silently returning HTML.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/login",
        "/guide",
        "/run",
        "/upload",
        "/alerts",
        "/analysis",
        "/history",
        "/manual"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}