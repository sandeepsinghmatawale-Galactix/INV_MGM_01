package com.barinventory.service;

import com.barinventory.entity.Bar;
import com.barinventory.repository.BarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarService {
    
    private final BarRepository barRepository;
    
    public List<Bar> getAllActiveBars() {
        return barRepository.findByActiveTrue();
    }
    
    public Bar getBarById(Long barId) {
        return barRepository.findById(barId)
            .orElseThrow(() -> new RuntimeException("Bar not found"));
    }
    
    @Transactional
    public Bar createBar(Bar bar) {
        if (barRepository.existsByBarName(bar.getBarName())) {
            throw new RuntimeException("Bar with this name already exists");
        }
        return barRepository.save(bar);
    }
    
    @Transactional
    public Bar updateBar(Long barId, Bar barDetails) {
        Bar bar = getBarById(barId);
        bar.setBarName(barDetails.getBarName());
        bar.setLocation(barDetails.getLocation());
        bar.setContactNumber(barDetails.getContactNumber());
        bar.setOwnerName(barDetails.getOwnerName());
        return barRepository.save(bar);
    }
    
    @Transactional
    public void deactivateBar(Long barId) {
        Bar bar = getBarById(barId);
        bar.setActive(false);
        barRepository.save(bar);
    }
}
