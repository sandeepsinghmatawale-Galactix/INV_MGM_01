package com.barinventory.service;

import com.barinventory.entity.InventorySession;
import com.barinventory.entity.SalesRecord;
import com.barinventory.repository.SalesRecordRepository;
import com.barinventory.repository.InventorySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final SalesRecordRepository salesRepository;
    private final InventorySessionRepository sessionRepository;
    
    /**
     * Get total sales for a session
     */
    public BigDecimal getSessionTotalSales(Long sessionId) {
        return salesRepository.getTotalRevenueBySession(sessionId);
    }
    
    /**
     * Get sales records for a date range
     */
    public List<SalesRecord> getSalesByDateRange(Long barId, 
                                                 LocalDateTime startDate, 
                                                 LocalDateTime endDate) {
        return salesRepository.findSalesByBarAndDateRange(barId, startDate, endDate);
    }
    
    /**
     * Get daily sales report
     */
    public Map<String, Object> getDailySalesReport(Long barId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<SalesRecord> sales = getSalesByDateRange(barId, startOfDay, endOfDay);
        
        BigDecimal totalRevenue = sales.stream()
            .map(SalesRecord::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = sales.stream()
            .map(SalesRecord::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);
        
        return Map.of(
            "date", date,
            "salesRecords", sales,
            "totalRevenue", totalRevenue,
            "totalCost", totalCost,
            "totalProfit", totalProfit
        );
    }
    
    /**
     * Get weekly sales report
     */
    public Map<String, Object> getWeeklySalesReport(Long barId, LocalDateTime weekStart) {
        LocalDateTime weekEnd = weekStart.plusDays(7);
        List<SalesRecord> sales = getSalesByDateRange(barId, weekStart, weekEnd);
        
        BigDecimal totalRevenue = sales.stream()
            .map(SalesRecord::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Map.of(
            "weekStart", weekStart,
            "weekEnd", weekEnd,
            "salesRecords", sales,
            "totalRevenue", totalRevenue
        );
    }
    
    /**
     * Get monthly sales report
     */
    public Map<String, Object> getMonthlySalesReport(Long barId, int year, int month) {
        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        
        List<SalesRecord> sales = getSalesByDateRange(barId, monthStart, monthEnd);
        
        BigDecimal totalRevenue = sales.stream()
            .map(SalesRecord::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Group by product
        Map<String, BigDecimal> productWiseSales = sales.stream()
            .collect(Collectors.groupingBy(
                s -> s.getProduct().getProductName(),
                Collectors.reducing(BigDecimal.ZERO,
                    SalesRecord::getTotalRevenue,
                    BigDecimal::add)
            ));
        
        return Map.of(
            "year", year,
            "month", month,
            "salesRecords", sales,
            "totalRevenue", totalRevenue,
            "productWiseSales", productWiseSales
        );
    }
    
    /**
     * Get audit trail for sessions
     */
    public List<InventorySession> getAuditTrail(Long barId, 
                                               LocalDateTime startDate, 
                                               LocalDateTime endDate) {
        return sessionRepository.findSessionsByBarAndDateRange(barId, startDate, endDate);
    }
    
    /**
     * Get product-wise sales summary
     */
    public Map<String, Object> getProductWiseSummary(Long barId, 
                                                    LocalDateTime startDate, 
                                                    LocalDateTime endDate) {
        List<SalesRecord> sales = getSalesByDateRange(barId, startDate, endDate);
        
        Map<String, Map<String, Object>> productSummary = sales.stream()
            .collect(Collectors.groupingBy(
                s -> s.getProduct().getProductName(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> Map.of(
                        "totalQuantity", list.stream()
                            .map(SalesRecord::getQuantitySold)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "totalRevenue", list.stream()
                            .map(SalesRecord::getTotalRevenue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "count", list.size()
                    )
                )
            ));
        
        return Map.of("productSummary", productSummary);
    }
}
