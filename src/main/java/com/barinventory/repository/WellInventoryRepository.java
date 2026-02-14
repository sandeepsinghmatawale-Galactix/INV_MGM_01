package com.barinventory.repository;

import com.barinventory.entity.WellInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WellInventoryRepository extends JpaRepository<WellInventory, Long> {
    
    List<WellInventory> findBySessionSessionId(Long sessionId);
    
    List<WellInventory> findBySessionSessionIdAndProductProductId(
        Long sessionId, Long productId);
    
    @Query("SELECT COALESCE(SUM(w.receivedFromDistribution), 0) FROM WellInventory w " +
           "WHERE w.session.sessionId = :sessionId AND w.product.productId = :productId")
    BigDecimal sumReceivedBySessionAndProduct(
        @Param("sessionId") Long sessionId, 
        @Param("productId") Long productId);
    
    @Query("SELECT COALESCE(SUM(w.consumed), 0) FROM WellInventory w " +
           "WHERE w.session.sessionId = :sessionId AND w.product.productId = :productId")
    BigDecimal sumConsumedBySessionAndProduct(
        @Param("sessionId") Long sessionId, 
        @Param("productId") Long productId);
}
