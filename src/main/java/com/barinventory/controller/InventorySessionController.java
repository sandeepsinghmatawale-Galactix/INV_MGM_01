package com.barinventory.controller;

import com.barinventory.entity.*;
import com.barinventory.service.InventorySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class InventorySessionController {
    
    private final InventorySessionService sessionService;
    
    @PostMapping("/initialize")
    public ResponseEntity<InventorySession> initializeSession(
            @RequestParam Long barId,
            @RequestParam String shiftType,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(sessionService.initializeSession(barId, shiftType, notes));
    }
    
    @PostMapping("/{sessionId}/stockroom")
    public ResponseEntity<Map<String, String>> saveStockroom(
            @PathVariable Long sessionId,
            @RequestBody List<StockroomInventory> inventories) {
        sessionService.saveStockroomInventory(sessionId, inventories);
        return ResponseEntity.ok(Map.of("message", "Stockroom inventory saved"));
    }
    
    @PostMapping("/{sessionId}/distribution/create")
    public ResponseEntity<Map<String, String>> createDistribution(@PathVariable Long sessionId) {
        sessionService.createDistributionRecords(sessionId);
        return ResponseEntity.ok(Map.of("message", "Distribution records created"));
    }
    
    @PostMapping("/{sessionId}/wells")
    public ResponseEntity<Map<String, String>> saveWells(
            @PathVariable Long sessionId,
            @RequestBody List<WellInventory> wellInventories) {
        sessionService.saveWellInventory(sessionId, wellInventories);
        return ResponseEntity.ok(Map.of("message", "Well inventory saved"));
    }
    
    @PostMapping("/{sessionId}/commit")
    public ResponseEntity<Map<String, String>> commitSession(@PathVariable Long sessionId) {
        try {
            sessionService.commitSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session committed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
   

    
    @PostMapping("/{sessionId}/rollback")
    public ResponseEntity<Map<String, String>> rollbackSession(
            @PathVariable Long sessionId,
            @RequestParam String reason) {
        sessionService.rollbackSession(sessionId, reason);
        return ResponseEntity.ok(Map.of("message", "Session rolled back"));
    }
    
    @GetMapping("/{sessionId}")
    public ResponseEntity<InventorySession> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }
    
    @GetMapping("/bar/{barId}")
    public ResponseEntity<List<InventorySession>> getSessionsByBar(@PathVariable Long barId) {
        return ResponseEntity.ok(sessionService.getSessionsByBar(barId));
    }
    
    @GetMapping("/bar/{barId}/daterange")
    public ResponseEntity<List<InventorySession>> getSessionsByDateRange(
            @PathVariable Long barId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return ResponseEntity.ok(sessionService.getSessionsByDateRange(barId, start, end));
    }
}
