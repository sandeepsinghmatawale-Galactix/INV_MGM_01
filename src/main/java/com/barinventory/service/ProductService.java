package com.barinventory.service;

import com.barinventory.entity.Product;
import com.barinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }
    
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.existsByProductName(product.getProductName())) {
            throw new RuntimeException("Product with this name already exists");
        }
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateProduct(Long productId, Product productDetails) {
        Product product = getProductById(productId);
        product.setProductName(productDetails.getProductName());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setVolumeML(productDetails.getVolumeML());
        product.setUnit(productDetails.getUnit());
        return productRepository.save(product);
    }
    
    @Transactional
    public void deactivateProduct(Long productId) {
        Product product = getProductById(productId);
        product.setActive(false);
        productRepository.save(product);
    }
}
