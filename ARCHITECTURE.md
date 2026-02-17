# System Architecture - Liquor Inventory Management System

## 🏗️ Architecture Overview

### Architectural Pattern: Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                     │
│  (Thymeleaf Views + REST Controllers + Web UI)         │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                   Business Logic Layer                  │
│        (Services with @Transactional methods)           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                   Data Access Layer                     │
│         (JPA Repositories + Entity Models)              │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                    Database Layer                       │
│                 (MySQL Database)                        │
└─────────────────────────────────────────────────────────┘
```

## 🔄 Transaction Flow Architecture

### Critical: Atomic Session Management

```
┌────────────────────────────────────────────────────────┐
│              @Transactional Boundary                   │
│                                                        │
│  ┌──────────────────────────────────────────────┐    │
│  │  1. Initialize Session (IN_PROGRESS)         │    │
│  └──────────────────────────────────────────────┘    │
│                      ↓                                │
│  ┌──────────────────────────────────────────────┐    │
│  │  2. Save Stockroom Inventory                 │    │
│  │     - Calculate: Transferred Out             │    │
│  └──────────────────────────────────────────────┘    │
│                      ↓                                │
│  ┌──────────────────────────────────────────────┐    │
│  │  3. Create Distribution Records              │    │
│  │     - Status: PENDING_ALLOCATION             │    │
│  └──────────────────────────────────────────────┘    │
│                      ↓                                │
│  ┌──────────────────────────────────────────────┐    │
│  │  4. Save Well Inventory                      │    │
│  │     - Update Distribution: ALLOCATED         │    │
│  │     - Calculate: Consumed                    │    │
│  └──────────────────────────────────────────────┘    │
│                      ↓                                │
│  ┌──────────────────────────────────────────────┐    │
│  │  5. Commit Session                           │    │
│  │     ✓ Validate all stages                    │    │
│  │     ✓ Generate sales records                 │    │
│  │     ✓ Status: COMPLETED                      │    │
│  │     OR                                        │    │
│  │     ✗ Rollback (Status: ROLLED_BACK)        │    │
│  └──────────────────────────────────────────────┘    │
│                                                        │
└────────────────────────────────────────────────────────┘
```

## 🗂️ Entity Relationship Diagram

```
┌─────────────┐           ┌──────────────┐
│    Bars     │◄─────────┤ Inventory    │
│             │    1:N   │  Sessions    │
│  bar_id (PK)│          │              │
│  bar_name   │          │ session_id   │
│  location   │          │ bar_id (FK)  │
│  active     │          │ status       │
└──────┬──────┘          │ shift_type   │
       │                 └──────┬───────┘
       │                        │
       │                        │ 1:N
       │                        ├──────────┬─────────────┬────────────┐
       │                        │          │             │            │
       │              ┌─────────▼────┐ ┌──▼──────┐ ┌───▼─────┐  ┌──▼─────┐
       │              │  Stockroom   │ │ Distri- │ │  Well   │  │ Sales  │
       │              │  Inventory   │ │ bution  │ │ Invent- │  │ Records│
       │              │              │ │ Records │ │  ory    │  │        │
       │              │ opening_stock│ │ quantity│ │ received│  │quantity│
       │              │ received     │ │ allocated│ │ consumed│ │ revenue│
       │              │ closing_stock│ │ status  │ │ well_id │  │ profit │
       │              │ transferred  │ └─────────┘ └─────────┘  └────────┘
       │              └──────────────┘
       │
       │1:N                              1:N
       ├───────────────────────┬──────────────────────────┐
       │                       │                          │
┌──────▼──────────┐    ┌──────▼────────┐      ┌─────────▼────────┐
│ BarProductPrice │    │   Products    │      │   Products       │
│                 │    │               │      │                  │
│ bar_id (FK)     │    │ product_id(PK)│◄────┤ product_id (PK)  │
│ product_id (FK) │    │ product_name  │      │ product_name     │
│ selling_price   │    │ category      │      │ category         │
│ cost_price      │    │ brand         │      │ volume_ml        │
│ active          │    │ volume_ml     │      │ unit             │
└─────────────────┘    │ unit          │      │ active           │
                       └───────────────┘      └──────────────────┘
```

## 🔐 Security Architecture

### 1. Transaction Isolation

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void commitSession(Long sessionId) {
    // Prevents dirty reads
    // Ensures data consistency
}
```

### 2. Validation Chain

```
Input Data
    ↓
Business Rule Validation
    ↓
Database Constraint Validation
    ↓
Transaction Boundary
    ↓
    ├─→ All Pass → COMMIT
    └─→ Any Fail → ROLLBACK
```

### 3. Audit Trail

Every action is logged:
- Session timestamps
- Status changes
- Validation errors
- User actions (when authentication added)

## 📊 Data Flow Diagram

### Complete Session Lifecycle

```
[Manager Starts Shift]
         ↓
    Initialize Session
    (Status: IN_PROGRESS)
         ↓
[Physical Stock Count]
         ↓
    Enter Stockroom Data
    ┌──────────────────┐
    │ Opening: 100     │
    │ Received: 50     │
    │ Closing: 120     │
    │ AUTO: Trans: 30  │
    └──────────────────┘
         ↓
    Create Distribution
    ┌──────────────────┐
    │ From Stock: 30   │
    │ Allocated: 0     │
    │ Unalloc: 30      │
    │ Status: PENDING  │
    └──────────────────┘
         ↓
[Allocate to Wells]
         ↓
    Save Well Data
    ┌──────────────────┐
    │ BAR_1: Recv: 20  │
    │ BAR_2: Recv: 10  │
    │ Total: 30 ✓      │
    └──────────────────┘
         ↓
    Update Distribution
    ┌──────────────────┐
    │ Allocated: 30    │
    │ Unalloc: 0       │
    │ Status: ALLOCATED│
    └──────────────────┘
         ↓
[End of Shift Count]
         ↓
    Calculate Consumed
    ┌──────────────────┐
    │ BAR_1: 15 sold   │
    │ BAR_2: 12 sold   │
    │ Total: 27        │
    └──────────────────┘
         ↓
    Commit Session
         ↓
    Validations
    ┌─────────────────────────────┐
    │ ✓ Stock → Distrib match     │
    │ ✓ Distrib → Wells match     │
    │ ✓ No unallocated stock      │
    └─────────────────────────────┘
         ↓
    Generate Sales
    ┌──────────────────┐
    │ 27 × ₹500        │
    │ = ₹13,500        │
    └──────────────────┘
         ↓
    Status: COMPLETED
```

## 🎯 Validation Architecture

### Three-Layer Validation

```
┌─────────────────────────────────────────┐
│        Layer 1: Input Validation        │
│  - Not null checks                      │
│  - Data type validation                 │
│  - Range validation                     │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│    Layer 2: Business Rule Validation    │
│  - Stock conservation law               │
│  - Distribution allocation completeness │
│  - Price configuration checks           │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│   Layer 3: Cross-Stage Validation       │
│  - Stockroom ↔ Distribution match       │
│  - Distribution ↔ Wells match           │
│  - Theft detection algorithms           │
└─────────────────────────────────────────┘
```

### Validation Examples

```java
// Validation 1: Stock Conservation
Opening + Received = Closing + Transferred
100 + 50 = 120 + 30 ✓

// Validation 2: Distribution Match
Stockroom.transferred = Distribution.quantityFromStockroom
30 = 30 ✓

// Validation 3: Allocation Completeness
Distribution.totalAllocated = SUM(Wells.received)
30 = (20 + 10) ✓

// Validation 4: No Leakage
Distribution.unallocated = 0
0 = 0 ✓
```

## 🚀 Performance Architecture

### 1. Database Connection Pooling

```
Application Threads (200)
         ↓
HikariCP Pool (20 connections)
         ↓
MySQL Database

Configuration:
- Max Pool Size: 20
- Min Idle: 5
- Connection Timeout: 30s
- Idle Timeout: 10min
```

### 2. Query Optimization Strategy

```sql
-- Use indexed lookups
SELECT * FROM inventory_sessions 
WHERE bar_id = ? AND status = ?
-- Index: (bar_id, status)

-- Batch processing
INSERT INTO well_inventory VALUES (...), (...), (...)
-- Hibernate batch_size: 20

-- Lazy loading for associations
@ManyToOne(fetch = FetchType.LAZY)
private Bar bar;
```

### 3. Caching Strategy (Future Enhancement)

```
┌──────────────────────────────────┐
│  Redis Cache Layer               │
│  - Active bars                   │
│  - Product catalog               │
│  - Price configurations          │
│  TTL: 1 hour                     │
└──────────────────────────────────┘
         ↓ Cache Miss
┌──────────────────────────────────┐
│  Database                        │
└──────────────────────────────────┘
```

## 📈 Scalability Architecture

### Horizontal Scaling

```
         Load Balancer
               ↓
    ┌──────────┼──────────┐
    ↓          ↓          ↓
App Server App Server App Server
    ↓          ↓          ↓
    └──────────┼──────────┘
               ↓
         MySQL Primary
               ↓
    ┌──────────┼──────────┐
    ↓          ↓          ↓
Replica-1  Replica-2  Replica-3
(Read)     (Read)     (Read)
```

### Multi-Tenancy Design

```java
// Current: Bar-level isolation
@Entity
@Table(name = "inventory_sessions")
public class InventorySession {
    @ManyToOne
    private Bar bar; // Tenant identifier
}

// Future: Organization-level
@Entity
@Table(name = "organizations")
public class Organization {
    @OneToMany
    private List<Bar> bars;
}
```

## 🔧 Technology Stack Details

### Backend Stack

```
Spring Boot 3.2.0
├── Spring Data JPA (Data Access)
├── Spring Web (REST APIs)
├── Spring Validation (Input validation)
├── Hibernate (ORM)
└── HikariCP (Connection Pooling)

Java 17
├── Records (Immutable DTOs)
├── Stream API (Data processing)
└── Optional (Null safety)

Lombok
├── @Data (Boilerplate reduction)
├── @Builder (Object creation)
└── @Slf4j (Logging)
```

### Database Stack

```
MySQL 8.0
├── InnoDB (Transaction support)
├── Foreign Keys (Referential integrity)
├── Indexes (Performance)
└── Views (Reporting)
```

### Frontend Stack

```
Thymeleaf
├── Server-side rendering
├── Form binding
└── Security integration

Bootstrap 5
├── Responsive design
├── Component library
└── Grid system

Bootstrap Icons
└── UI icons
```

## 🎨 Design Patterns Used

### 1. Repository Pattern
```java
public interface InventorySessionRepository 
    extends JpaRepository<InventorySession, Long> {
    // Abstraction over data access
}
```

### 2. Service Layer Pattern
```java
@Service
@Transactional
public class InventorySessionService {
    // Business logic encapsulation
}
```

### 3. Builder Pattern
```java
InventorySession session = InventorySession.builder()
    .bar(bar)
    .status(SessionStatus.IN_PROGRESS)
    .build();
```

### 4. Strategy Pattern (Future)
```java
public interface ValidationStrategy {
    boolean validate(InventorySession session);
}

public class StockroomValidationStrategy 
    implements ValidationStrategy { }

public class DistributionValidationStrategy 
    implements ValidationStrategy { }
```

## 📊 Monitoring Architecture

### Application Metrics

```
Spring Boot Actuator
├── /actuator/health (Health check)
├── /actuator/metrics (App metrics)
└── /actuator/info (App info)

Metrics Collected:
- JVM memory usage
- Thread count
- HTTP request latency
- Database connection pool stats
- Transaction success/failure rate
```

### Business Metrics

```sql
-- Sessions per day
SELECT DATE(session_start_time), COUNT(*)
FROM inventory_sessions
GROUP BY DATE(session_start_time);

-- Average session completion time
SELECT AVG(TIMESTAMPDIFF(MINUTE, session_start_time, session_end_time))
FROM inventory_sessions
WHERE status = 'COMPLETED';

-- Validation failure rate
SELECT 
    (SELECT COUNT(*) FROM inventory_sessions WHERE status = 'ROLLED_BACK') /
    (SELECT COUNT(*) FROM inventory_sessions) * 100 as failure_rate;
```

## 🔄 Future Enhancements

### Phase 2: Advanced Features

1. **Mobile App**
   - Barcode scanning
   - Offline mode
   - Real-time sync

2. **Analytics Dashboard**
   - Predictive analytics
   - Theft pattern detection
   - Demand forecasting

3. **Integration**
   - POS system integration
   - Accounting software sync
   - Government reporting

4. **Advanced Security**
   - Role-based access control
   - Multi-factor authentication
   - Audit logging

### Microservices Architecture (Phase 3)

```
API Gateway
    ↓
├─→ Inventory Service
├─→ Sales Service
├─→ Reporting Service
├─→ User Service
└─→ Notification Service
```

---

**Architecture Version:** 1.0  
**Last Updated:** February 2024  
**Status:** Production Ready
