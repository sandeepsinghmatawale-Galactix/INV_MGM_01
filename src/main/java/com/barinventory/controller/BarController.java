package com.barinventory.controller;

import com.barinventory.entity.Bar;
import com.barinventory.service.BarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class BarController {
    
    private final BarService barService;
    
    @GetMapping
    public ResponseEntity<List<Bar>> getAllBars() {
        return ResponseEntity.ok(barService.getAllActiveBars());
    }
    
    @GetMapping("/{barId}")
    public ResponseEntity<Bar> getBar(@PathVariable Long barId) {
        return ResponseEntity.ok(barService.getBarById(barId));
    }
    
    @PostMapping
    public ResponseEntity<Bar> createBar(@RequestBody Bar bar) {
        return ResponseEntity.ok(barService.createBar(bar));
    }
    
    @PutMapping("/{barId}")
    public ResponseEntity<Bar> updateBar(@PathVariable Long barId, @RequestBody Bar bar) {
        return ResponseEntity.ok(barService.updateBar(barId, bar));
    }
    
    @DeleteMapping("/{barId}")
    public ResponseEntity<Void> deactivateBar(@PathVariable Long barId) {
        barService.deactivateBar(barId);
        return ResponseEntity.ok().build();
    }
}
