package com.barinventory.repository;

import com.barinventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByActiveTrue();
    
    List<Product> findByCategory(String category);
    
    Optional<Product> findByProductName(String productName);
    
    boolean existsByProductName(String productName);
}
