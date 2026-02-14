package com.barinventory.entity;

import java.time.LocalDateTime;

import com.barinventory.enums.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventorySessionDTO {
    private Long sessionId;
    private Long barId;
    private String barName;
    private LocalDateTime sessionStartTime;
    private SessionStatus status;
    private String shiftType;
    private String notes;
    // DON'T include nested collections that reference back
}