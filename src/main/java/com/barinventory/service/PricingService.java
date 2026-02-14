package com.barinventory.service;

import com.barinventory.entity.Bar;
import com.barinventory.entity.BarProductPrice;
import com.barinventory.entity.Product;
import com.barinventory.repository.BarProductPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {
    
    private final BarProductPriceRepository priceRepository;
    private final BarService barService;
    private final ProductService productService;
    
    public List<BarProductPrice> getPricesByBar(Long barId) {
        return priceRepository.findByBarBarIdAndActiveTrue(barId);
    }
    
    public BarProductPrice getPrice(Long barId, Long productId) {
        return priceRepository.findByBarBarIdAndProductProductId(barId, productId)
            .orElseThrow(() -> new RuntimeException("Price not configured for this product"));
    }
    
    @Transactional
    public BarProductPrice setPrice(Long barId, Long productId, BarProductPrice priceDetails) {
        Bar bar = barService.getBarById(barId);
        Product product = productService.getProductById(productId);
        
        BarProductPrice price = priceRepository
            .findByBarBarIdAndProductProductId(barId, productId)
            .orElse(BarProductPrice.builder()
                .bar(bar)
                .product(product)
                .build());
        
        price.setSellingPrice(priceDetails.getSellingPrice());
        price.setCostPrice(priceDetails.getCostPrice());
        price.setActive(true);
        
        return priceRepository.save(price);
    }
    
    @Transactional
    public void deactivatePrice(Long priceId) {
        BarProductPrice price = priceRepository.findById(priceId)
            .orElseThrow(() -> new RuntimeException("Price not found"));
        price.setActive(false);
        priceRepository.save(price);
    }
}
