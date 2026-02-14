package com.barinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "sales_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InventorySession session;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantitySold = BigDecimal.ZERO; // Sum of consumed from all wells
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPricePerUnit = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO; // quantitySold × sellingPricePerUnit
    
    @Column(precision = 10, scale = 2)
    private BigDecimal costPricePerUnit = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO; // totalRevenue - totalCost
    
    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        this.totalRevenue = this.quantitySold.multiply(this.sellingPricePerUnit);
        this.totalCost = this.quantitySold.multiply(this.costPricePerUnit);
        this.profit = this.totalRevenue.subtract(this.totalCost);
    }
}
