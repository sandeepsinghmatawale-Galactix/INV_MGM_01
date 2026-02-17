package com.barinventory.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "bars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sessions", "productPrices"})
public class Bar {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long barId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String barName;
    
    @Column(length = 200)
    private String location;
    
    @Column(length = 20)
    private String contactNumber;
    
    @Column(length = 100)
    private String ownerName;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @OneToMany(mappedBy = "bar", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BarProductPrice> productPrices;

    

    
    @OneToMany(mappedBy = "bar")
    @JsonIgnore  // ✅ Add this
    private List<InventorySession> sessions;
    
}
