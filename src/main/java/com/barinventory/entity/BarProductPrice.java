package com.barinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "bar_product_prices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"bar_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarProductPrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bar_id", nullable = false)
    private Bar bar;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice; // Price per bottle/unit
    
    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice; // Optional: for profit calculation
    
    @Column(nullable = false)
    private Boolean active = true;
}
