package com.barinventory.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.barinventory.enums.SessionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "inventory_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "stockroomInventories", "wellInventories", "distributionRecords", "salesRecords" }) //
@Builder
public class InventorySession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "bar_id", nullable = false)
	private Bar bar;

	@Column(nullable = false)
	private LocalDateTime sessionStartTime;

	@Column
	private LocalDateTime sessionEndTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status = SessionStatus.IN_PROGRESS; // ✅ Remove __

	@Column(length = 20)
	private String shiftType;

	@Column(length = 500)
	private String notes;

	@Column(length = 1000)
	private String validationErrors;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private List<SalesRecord> salesRecords;

	
	@OneToMany(mappedBy = "session")
	@JsonManagedReference
	private List<StockroomInventory> stockroomInventories;


	@OneToMany(mappedBy = "session")
	@JsonIgnore
	private List<WellInventory> wellInventories;

	@OneToMany(mappedBy = "session")
	@JsonIgnore
	private List<DistributionRecord> distributionRecords;
}