package com.barinventory.repository;

import com.barinventory.entity.StockroomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockroomInventoryRepository extends JpaRepository<StockroomInventory, Long> {
    
    List<StockroomInventory> findBySessionSessionId(Long sessionId);
    
    Optional<StockroomInventory> findBySessionSessionIdAndProductProductId(
        Long sessionId, Long productId);
}
