package com.acrom.memory.controller;

import com.acrom.memory.model.Score;
import com.acrom.memory.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MemoryController {

    private final MemoryService service;

    public MemoryController(MemoryService service) {
        this.service = service;
    }

    @PostMapping("/games")
    public Map<String, Object> newGame(@RequestBody(required = false) Map<String, Object> body) {
        String player = body != null && body.get("player") != null ? String.valueOf(body.get("player")) : null;
        Integer pairs = null;
        if (body != null && body.get("pairs") != null) {
            pairs = ((Number) body.get("pairs")).intValue();
        }
        return service.create(player, pairs);
    }

    @GetMapping("/games/{id}")
    public Map<String, Object> state(@PathVariable Long id) {
        return service.state(id);
    }

    @PostMapping("/games/{id}/flip")
    public Map<String, Object> flip(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer index = null;
        if (body.get("index") != null) {
            index = ((Number) body.get("index")).intValue();
        }
        return service.flip(id, index);
    }

    @PostMapping("/games/{id}/finish")
    public Map<String, Object> finish(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Integer seconds = null;
        if (body != null && body.get("seconds") != null) {
            seconds = ((Number) body.get("seconds")).intValue();
        }
        return service.finish(id, seconds);
    }

    @GetMapping("/leaderboard")
    public List<Score> leaderboard() {
        return service.leaderboard();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> bad(IllegalArgumentException ex) {
        Map<String, String> err = new java.util.LinkedHashMap<>();
        err.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(err);
    }
}
