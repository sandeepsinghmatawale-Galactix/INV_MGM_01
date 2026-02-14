package com.barinventory.repository;

import com.barinventory.entity.DistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Long> {
    
    List<DistributionRecord> findBySessionSessionId(Long sessionId);
    
    Optional<DistributionRecord> findBySessionSessionIdAndProductProductId(
        Long sessionId, Long productId);
}
