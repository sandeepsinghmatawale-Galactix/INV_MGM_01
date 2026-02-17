package com.barinventory.repository;

import com.barinventory.entity.BarProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarProductPriceRepository extends JpaRepository<BarProductPrice, Long> {
    
    List<BarProductPrice> findByBarBarId(Long barId);
    
    Optional<BarProductPrice> findByBarBarIdAndProductProductId(Long barId, Long productId);
    
    List<BarProductPrice> findByBarBarIdAndActiveTrue(Long barId);
}
