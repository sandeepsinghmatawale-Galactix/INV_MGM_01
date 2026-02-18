package com.barinventory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.entity.Bar;
import com.barinventory.entity.BarProductPrice;
import com.barinventory.entity.DistributionRecord;
import com.barinventory.entity.InventorySession;
import com.barinventory.entity.Product;
import com.barinventory.entity.SalesRecord;
import com.barinventory.entity.StockroomInventory;
import com.barinventory.entity.WellInventory;
import com.barinventory.enums.DistributionStatus;
import com.barinventory.enums.SessionStatus;
import com.barinventory.repository.BarProductPriceRepository;
import com.barinventory.repository.BarRepository;
import com.barinventory.repository.DistributionRecordRepository;
import com.barinventory.repository.InventorySessionRepository;
import com.barinventory.repository.SalesRecordRepository;
import com.barinventory.repository.StockroomInventoryRepository;
import com.barinventory.repository.WellInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySessionService {
    
    private final InventorySessionRepository sessionRepository;
    private final StockroomInventoryRepository stockroomRepository;
    private final DistributionRecordRepository distributionRepository;
    private final WellInventoryRepository wellRepository;
    private final SalesRecordRepository salesRepository;
    private final BarRepository barRepository;
    private final BarProductPriceRepository priceRepository;
    private final ProductService productService;
    
    /**
     * Initialize a new inventory session for a bar
     */
    @Transactional
    public InventorySession initializeSession(Long barId, String shiftType, String notes) {
        if (barId == null) throw new IllegalArgumentException("Bar ID cannot be null");

        Bar bar = barRepository.findById(barId)
            .orElseThrow(() -> new RuntimeException("Bar not found"));

        Optional<InventorySession> existingSession = sessionRepository
            .findFirstByBarBarIdAndStatusOrderBySessionStartTimeDesc(barId, SessionStatus.IN_PROGRESS);

        if (existingSession.isPresent()) {
            // ✅ FIX: use findByIdWithBar to ensure bar is loaded
            return sessionRepository.findByIdWithBar(existingSession.get().getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
        }

        InventorySession session = InventorySession.builder()
            .bar(bar)
            .sessionStartTime(LocalDateTime.now())
            .status(SessionStatus.IN_PROGRESS)
            .shiftType(shiftType)
            .notes(notes)
            .stockroomInventories(new ArrayList<>())
            .distributionRecords(new ArrayList<>())
            .wellInventories(new ArrayList<>())
            .salesRecords(new ArrayList<>())
            .build();

        return sessionRepository.save(session);
    }
    
 /*   public InventorySession getSession(Long sessionId) {
        return sessionRepository.findByIdWithBar(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    }*/
    
    
    
    public InventorySession getSession(Long sessionId) {
        return sessionRepository.findByIdWithBar(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }
    
    



 public Optional<InventorySession> getSessionById(Long sessionId) {
	    return sessionRepository.findByIdWithBar(sessionId);
	}

    
    /**
     * STAGE 1: Save stockroom inventory
     */
  
    
    @Transactional
    public void saveStockroomInventory(Long sessionId, List<StockroomInventory> inventories) {
        InventorySession session = getSessionInProgress(sessionId);

        // ✅ Delete existing stockroom records before re-saving to avoid duplicates
        stockroomRepository.deleteBySessionSessionId(sessionId);

        for (StockroomInventory inventory : inventories) {
            inventory.setSession(session);
            stockroomRepository.save(inventory);
        }

        log.info("Saved {} stockroom inventory records for session {}", 
            inventories.size(), sessionId);
    }
    
    /**
     * STAGE 2: Create distribution records from stockroom transferred quantities
     */
   
    
    @Transactional
    public void createDistributionRecords(Long sessionId) {
        InventorySession session = getSessionInProgress(sessionId);
        List<StockroomInventory> stockroomInventories = 
            stockroomRepository.findBySessionSessionId(sessionId);

        for (StockroomInventory stockroom : stockroomInventories) {
            if (stockroom.getTransferredOut().compareTo(BigDecimal.ZERO) > 0) {

                // ✅ Check if distribution record already exists for this session+product
                Optional<DistributionRecord> existing = distributionRepository
                    .findBySessionSessionIdAndProductProductId(
                        sessionId, stockroom.getProduct().getProductId());

                if (existing.isPresent()) {
                    // ✅ Update existing record instead of creating duplicate
                    DistributionRecord dr = existing.get();
                    dr.setQuantityFromStockroom(stockroom.getTransferredOut());
                    dr.setTotalAllocated(BigDecimal.ZERO);
                    dr.setUnallocated(stockroom.getTransferredOut());
                    dr.setStatus(DistributionStatus.PENDING_ALLOCATION);
                    distributionRepository.save(dr);
                } else {
                    // ✅ Create new only if doesn't exist
                    DistributionRecord distribution = DistributionRecord.builder()
                        .session(session)
                        .product(stockroom.getProduct())
                        .quantityFromStockroom(stockroom.getTransferredOut())
                        .totalAllocated(BigDecimal.ZERO)
                        .unallocated(stockroom.getTransferredOut())
                        .status(DistributionStatus.PENDING_ALLOCATION)
                        .build();
                    distributionRepository.save(distribution);
                }
            }
        }

        log.info("Created/updated distribution records for session {}", sessionId);
    }
    
    /**
     * STAGE 3: Save well inventory (allocation to wells)
     */
  
    
    /**
     * Update distribution record when stock is allocated to wells
     */
    private void updateDistributionAllocation(Long sessionId, Long productId, BigDecimal quantity) {
        DistributionRecord distribution = distributionRepository
            .findBySessionSessionIdAndProductProductId(sessionId, productId)
            .orElseThrow(() -> new RuntimeException("Distribution record not found"));
        
        BigDecimal newTotal = distribution.getTotalAllocated().add(quantity);
        distribution.setTotalAllocated(newTotal);
        distributionRepository.save(distribution);
    }
    
    /**
     * FINAL STAGE: Commit session after validations
     */
    @Transactional
    public void commitSession(Long sessionId) {
        InventorySession session = getSessionInProgress(sessionId);
        
        // Perform all validations
        StringBuilder errors = new StringBuilder();
        
        
        // Validation 1: Stockroom transferred = Distribution total
        if (!validateStockroomToDistribution(sessionId, errors)) {
            rollbackSession(sessionId, errors.toString());
            throw new RuntimeException("Validation failed: " + errors.toString());
        }
        
        // Validation 2: Distribution allocated = Wells received
        if (!validateDistributionToWells(sessionId, errors)) {
            rollbackSession(sessionId, errors.toString());
            throw new RuntimeException("Validation failed: " + errors.toString());
        }
        
        // Validation 3: No unallocated stock in distribution
        if (!validateNoUnallocatedStock(sessionId, errors)) {
            rollbackSession(sessionId, errors.toString());
            throw new RuntimeException("Validation failed: " + errors.toString());
        }
     // Before generateSalesRecords(sessionId):
        validatePricesExist(sessionId, session);
        
        // All validations passed - generate sales and commit
        generateSalesRecords(sessionId);
        
        
        session.setStatus(SessionStatus.COMPLETED);
        session.setSessionEndTime(LocalDateTime.now());
        sessionRepository.save(session);
        
        log.info("Session {} committed successfully", sessionId);
    }
    
    private void validatePricesExist(Long sessionId, InventorySession session) {
        List<WellInventory> wells = wellRepository.findBySessionSessionId(sessionId);
        for (WellInventory w : wells) {
            priceRepository.findByBarBarIdAndProductProductId(
                session.getBar().getBarId(), w.getProduct().getProductId())
                .orElseThrow(() -> new RuntimeException(
                    "No price configured for: " + w.getProduct().getProductName()));
        }
    }
    
    /**
     * Validation 1: Stockroom transferred must equal distribution total
     */
    private boolean validateStockroomToDistribution(Long sessionId, StringBuilder errors) {
        List<StockroomInventory> stockrooms = stockroomRepository.findBySessionSessionId(sessionId);
        List<DistributionRecord> distributions = distributionRepository.findBySessionSessionId(sessionId);
        
        for (StockroomInventory stockroom : stockrooms) {
            BigDecimal transferred = stockroom.getTransferredOut();
            
            DistributionRecord distribution = distributions.stream()
                .filter(d -> d.getProduct().getProductId().equals(stockroom.getProduct().getProductId()))
                .findFirst()
                .orElse(null);
            
            if (distribution == null && transferred.compareTo(BigDecimal.ZERO) > 0) {
                errors.append("Product ").append(stockroom.getProduct().getProductName())
                    .append(": No distribution record found for transferred stock. ");
                return false;
            }
            
            if (distribution != null && 
                transferred.compareTo(distribution.getQuantityFromStockroom()) != 0) {
                errors.append("Product ").append(stockroom.getProduct().getProductName())
                    .append(": Stockroom transferred (").append(transferred)
                    .append(") != Distribution quantity (")
                    .append(distribution.getQuantityFromStockroom()).append("). ");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validation 2: Distribution allocated must equal wells received
     */
    private boolean validateDistributionToWells(Long sessionId, StringBuilder errors) {
        List<DistributionRecord> distributions = distributionRepository.findBySessionSessionId(sessionId);
        
        for (DistributionRecord distribution : distributions) {
            BigDecimal totalWellsReceived = wellRepository.sumReceivedBySessionAndProduct(
                sessionId, distribution.getProduct().getProductId());
            
            if (distribution.getTotalAllocated().compareTo(totalWellsReceived) != 0) {
                errors.append("Product ").append(distribution.getProduct().getProductName())
                    .append(": Distribution allocated (").append(distribution.getTotalAllocated())
                    .append(") != Wells received (").append(totalWellsReceived).append("). ");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validation 3: No stock should remain unallocated
     */
    private boolean validateNoUnallocatedStock(Long sessionId, StringBuilder errors) {
        List<DistributionRecord> distributions = distributionRepository.findBySessionSessionId(sessionId);
        
        for (DistributionRecord distribution : distributions) {
            if (distribution.getUnallocated().compareTo(BigDecimal.ZERO) > 0) {
                errors.append("Product ").append(distribution.getProduct().getProductName())
                    .append(": Unallocated stock remaining (")
                    .append(distribution.getUnallocated()).append(" units). ");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Generate sales records from consumed quantities
     */
    private void generateSalesRecords(Long sessionId) {
        InventorySession session = sessionRepository.findByIdWithBar(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        List<WellInventory> wellInventories = wellRepository.findBySessionSessionId(sessionId);

        Map<Long, BigDecimal> consumed = wellInventories.stream()
            .collect(Collectors.groupingBy(
                w -> w.getProduct().getProductId(),
                Collectors.reducing(BigDecimal.ZERO, WellInventory::getConsumed, BigDecimal::add)
            ));

        for (Map.Entry<Long, BigDecimal> entry : consumed.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal totalConsumed = entry.getValue();
            if (totalConsumed.compareTo(BigDecimal.ZERO) <= 0) continue;

            Product product = wellInventories.stream()
                .filter(w -> w.getProduct().getProductId().equals(productId))
                .findFirst().get().getProduct();

            BarProductPrice price = priceRepository
                .findByBarBarIdAndProductProductId(session.getBar().getBarId(), productId)
                .orElseThrow(() -> new RuntimeException(
                    "Price not set for product: " + product.getProductName() +
                    " at bar: " + session.getBar().getBarName()));

            SalesRecord sales = SalesRecord.builder()
                .session(session)
                .product(product)
                .quantitySold(totalConsumed)
                .sellingPricePerUnit(price.getSellingPrice())
                .costPricePerUnit(price.getCostPrice() != null ? price.getCostPrice() : BigDecimal.ZERO)
                .build();

            salesRepository.save(sales);
            log.info("Saved sales record: product={}, qty={}", product.getProductName(), totalConsumed);
        }
    }
    /**
     * Rollback session in case of validation failure
     */
    @Transactional
    public void rollbackSession(Long sessionId, String errorMessage) {
        InventorySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setStatus(SessionStatus.ROLLED_BACK);
        session.setSessionEndTime(LocalDateTime.now());
        session.setValidationErrors(errorMessage);
        sessionRepository.save(session);
        
        log.error("Session {} rolled back: {}", sessionId, errorMessage);
    }
    
    /**
     * Get session and verify it's in progress
     */
    private InventorySession getSessionInProgress(Long sessionId) {
        // ✅ FIX: use findByIdWithBar instead of findById
        InventorySession session = sessionRepository.findByIdWithBar(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Session is not in progress");
        }
        return session;
    }
    
    /**
     * Get session by ID
     */
 
    /**
     * Get all sessions for a bar
     */
    public List<InventorySession> getSessionsByBar(Long barId) {
        return sessionRepository.findByBarBarIdOrderBySessionStartTimeDesc(barId);
    }
    
    /**
     * Get sessions by date range
     */
    public List<InventorySession> getSessionsByDateRange(Long barId, 
                                                         LocalDateTime startDate, 
                                                         LocalDateTime endDate) {
        return sessionRepository.findSessionsByBarAndDateRange(barId, startDate, endDate);
    }
    
 // 1. Build productId → quantityFromStockroom map for pre-filling received
    public Map<Long, BigDecimal> getDistributionMapForSession(Long sessionId) {
        List<DistributionRecord> distributions =
                distributionRepository.findBySessionSessionId(sessionId);

        return distributions.stream()
                .collect(Collectors.toMap(
                        d -> d.getProduct().getProductId(),
                        DistributionRecord::getQuantityFromStockroom,
                        BigDecimal::add
                ));
    }

    // 2. Parse form data and save WellInventory records
    @Transactional
    public void saveWellInventoryFromForm(Long sessionId, Map<String, String> formData) {

        InventorySession session = getSessionInProgress(sessionId);
        List<Product> products = productService.getAllActiveProducts();

        // Clear existing well records to avoid duplicates on re-save
        wellRepository.deleteBySessionSessionId(sessionId);

        // ✅ FIX 3: Reset all distribution allocations before recalculating
        List<DistributionRecord> distributions = distributionRepository.findBySessionSessionId(sessionId);
        for (DistributionRecord dr : distributions) {
            dr.setTotalAllocated(BigDecimal.ZERO);
            distributionRepository.save(dr);
        }

        List<WellInventory> wellInventories = new ArrayList<>();

        for (Product product : products) {
            for (String wellName : List.of("BAR_1", "BAR_2", "SERVICE_BAR")) {

                String key = product.getProductId() + "_" + wellName;

                BigDecimal opening  = parseDecimal(formData.get("opening_"  + key));
                BigDecimal received = parseDecimal(formData.get("received_" + key));
                BigDecimal closing  = parseDecimal(formData.get("closing_"  + key));
                BigDecimal consumed = opening.add(received).subtract(closing);

                // ✅ FIX 1: Save record if ANY field has data, not just when consumed > 0
                boolean hasAnyData = opening.compareTo(BigDecimal.ZERO)  > 0
                                  || received.compareTo(BigDecimal.ZERO) > 0
                                  || closing.compareTo(BigDecimal.ZERO)  > 0;

                if (hasAnyData) {
                    // ✅ Guard against negative consumed (e.g. closing > opening + received)
                    BigDecimal consumedFinal = consumed.compareTo(BigDecimal.ZERO) >= 0
                            ? consumed : BigDecimal.ZERO;

                    WellInventory wi = WellInventory.builder()
                            .session(session)
                            .product(product)
                            .wellName(wellName)
                            .openingStock(opening)
                            .receivedFromDistribution(received)
                            .closingStock(closing)
                            .consumed(consumedFinal)
                            .build();

                    wellInventories.add(wi);
                    log.debug("Well record queued: product={}, well={}, opening={}, received={}, closing={}, consumed={}",
                            product.getProductName(), wellName, opening, received, closing, consumedFinal);
                }
            }
        }

        // ✅ This now also handles updateDistributionAllocation internally
        saveWellInventory(sessionId, wellInventories);

        log.info("saveWellInventoryFromForm complete: {} well records saved for session {}",
                wellInventories.size(), sessionId);
    }
    
    @Transactional
    public void saveWellInventory(Long sessionId, List<WellInventory> wellInventories) {
        InventorySession session = getSessionInProgress(sessionId);

        for (WellInventory wellInventory : wellInventories) {
            wellInventory.setSession(session);
            wellRepository.save(wellInventory);

            // Only update allocation if received > 0
            if (wellInventory.getReceivedFromDistribution().compareTo(BigDecimal.ZERO) > 0) {
                updateDistributionAllocation(sessionId,
                        wellInventory.getProduct().getProductId(),
                        wellInventory.getReceivedFromDistribution());
            }
        }

        log.info("Saved {} well inventory records for session {}", wellInventories.size(), sessionId);
    }

    private BigDecimal parseDecimal(String val) {
        return (val != null && !val.isEmpty()) ? new BigDecimal(val) : BigDecimal.ZERO;
    }
}
