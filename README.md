# Liquor Inventory Management System

A comprehensive inventory tracking system for multi-bar liquor management in India with built-in theft prevention and transactional integrity.

## 🎯 Business Problem Solved

Indian bars face significant challenges:
- **Theft**: Stock disappearing between stockroom and serving areas
- **Accountability**: No clear audit trail of who handled what stock
- **Reconciliation**: Manual calculations prone to errors
- **Multi-location**: Different bars with different pricing structures

## 🚀 Key Features

### 1. Three-Stage Inventory Tracking
```
Stockroom → Distribution → Wells → Sales
```

- **Stage 1 (Stockroom)**: Opening + Received - Closing = Transferred Out
- **Stage 2 (Distribution)**: Critical control point - ALL stock must be allocated
- **Stage 3 (Wells)**: Opening + Received - Closing = Consumed
- **Stage 4 (Sales)**: Consumed × Price = Revenue

### 2. Theft Prevention
- Distribution table acts as checkpoint
- Stock cannot "vanish" between stages
- 100% allocation required before commit
- Mismatch detection alerts

### 3. Atomic Transactions
```java
@Transactional
public void commitSession(Long sessionId) {
    // Either ALL validations pass and data commits
    // OR everything rolls back (no partial saves)
}
```

### 4. Multi-Bar Support
- Each bar operates independently
- Bar-specific pricing
- Separate inventory sessions
- Consolidated reporting

### 5. Comprehensive Reporting
- Daily/Weekly/Monthly sales
- Product-wise analysis
- Profit calculations
- Audit trails

## 📋 System Requirements

- **Java**: 17 or higher
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Spring Boot**: 3.2.0

## 🛠️ Installation & Setup

### 1. Database Setup

```sql
CREATE DATABASE liquor_inventory_db;
CREATE USER 'liquorapp'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON liquor_inventory_db.* TO 'liquorapp'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/liquor_inventory_db
spring.datasource.username=liquorapp
spring.datasource.password=secure_password
```

### 3. Build & Run

```bash
# Clone/Download project
cd liquor-inventory-system

# Build with Maven
mvn clean install

# Run application
mvn spring-boot:run

# Or run JAR
java -jar target/liquor-inventory-system-1.0.0.jar
```

### 4. Access Application

- **Web UI**: http://localhost:8080
- **API Docs**: http://localhost:8080/api

## 📊 Database Schema

### Core Tables

1. **bars** - Bar master data
2. **products** - Liquor products catalog
3. **bar_product_prices** - Bar-specific pricing
4. **inventory_sessions** - Daily shift sessions
5. **stockroom_inventory** - Stage 1 data
6. **distribution_records** - Stage 2 (control point)
7. **well_inventory** - Stage 3 data
8. **sales_records** - Revenue calculations

## 🔄 Complete Workflow

### Initialization
```bash
POST /api/sessions/initialize?barId=1&shiftType=EVENING
```

### Stage 1: Stockroom
```json
POST /api/sessions/1/stockroom
[
  {
    "product": {"productId": 1},
    "openingStock": 100,
    "receivedStock": 50,
    "closingStock": 120,
    "remarks": "Physical count done"
  }
]
```
**Calculated**: transferredOut = 100 + 50 - 120 = **30 bottles**

### Stage 2: Distribution
```bash
POST /api/sessions/1/distribution/create
```
Creates distribution records with 30 bottles pending allocation.

### Stage 3: Wells Allocation
```json
POST /api/sessions/1/wells
[
  {
    "product": {"productId": 1},
    "wellName": "BAR_1",
    "openingStock": 10,
    "receivedFromDistribution": 20,
    "closingStock": 5
  },
  {
    "product": {"productId": 1},
    "wellName": "BAR_2",
    "openingStock": 5,
    "receivedFromDistribution": 10,
    "closingStock": 8
  }
]
```
**Total allocated**: 20 + 10 = **30 bottles** ✅ Matches distribution

### Commit with Validations
```bash
POST /api/sessions/1/commit
```

**Validation Checks**:
1. ✅ Stockroom transferred (30) = Distribution total (30)
2. ✅ Distribution allocated (30) = Wells received (30)
3. ✅ No unallocated stock remaining (0)

**If all pass**:
- Calculates consumed = (10+20-5) + (5+10-8) = **32 bottles sold**
- Generates sales: 32 × ₹500 = **₹16,000 revenue**
- Status → COMPLETED

**If any fails**:
- Status → ROLLED_BACK
- Error message logged
- No data committed

## 🎯 API Endpoints

### Session Management
- `POST /api/sessions/initialize` - Start new session
- `POST /api/sessions/{id}/stockroom` - Save stockroom data
- `POST /api/sessions/{id}/distribution/create` - Create distribution
- `POST /api/sessions/{id}/wells` - Save wells data
- `POST /api/sessions/{id}/commit` - Commit session
- `GET /api/sessions/{id}` - Get session details

### Master Data
- `GET /api/bars` - List all bars
- `POST /api/bars` - Create bar
- `GET /api/products` - List products
- `POST /api/products` - Create product

### Pricing
- `GET /api/pricing/{barId}` - Get bar prices
- `POST /api/pricing/{barId}/{productId}` - Set price

## 📱 Web Interface

### Dashboard
- View all bars
- Quick access to sessions, pricing, reports

### Session Management
- Initialize new session
- Step-by-step data entry
- Real-time validation feedback
- Commit/rollback controls

### Reports
- Daily sales summary
- Weekly/monthly aggregations
- Product-wise breakdown
- Audit trail view

## 🔒 Security Features

### Transactional Integrity
```java
@Transactional
public void commitSession(Long sessionId) {
    // Atomic operation - all or nothing
    validate();
    calculateSales();
    commit();
}
```

### Validation Rules
1. **Stock Conservation**: Opening + Received = Closing + Transferred
2. **Distribution Match**: Stockroom out = Distribution in
3. **Allocation Complete**: Distribution allocated = Wells received
4. **No Leakage**: Unallocated stock = 0

### Audit Trail
- Every session timestamped
- Status tracking (IN_PROGRESS, COMPLETED, ROLLED_BACK)
- Error messages logged
- Complete history maintained

## 📈 Reporting Examples

### Daily Sales Report
```java
GET /api/reports/{barId}/daily?date=2024-02-11
```
Returns:
- Total revenue
- Total cost
- Total profit
- Product-wise breakdown

### Monthly Analysis
```java
GET /api/reports/{barId}/monthly?year=2024&month=2
```
Returns:
- Monthly revenue
- Top selling products
- Category-wise sales

## 🔧 Customization

### Adding New Product Categories
```java
// ProductService.java
public static final String[] CATEGORIES = {
    "Whisky", "Vodka", "Rum", "Gin", "Beer", 
    "Wine", "Tequila", "Brandy", "Liqueur"
};
```

### Adding Custom Validations
```java
// InventorySessionService.java
private boolean validateCustomRule(Long sessionId, StringBuilder errors) {
    // Add your business logic
    return true;
}
```

## 🐛 Troubleshooting

### Session Won't Commit
**Error**: "Validation failed: Distribution allocated != Wells received"

**Solution**: Check that sum of `receivedFromDistribution` across all wells equals `totalAllocated` in distribution table.

### Price Not Found Error
**Error**: "Price not configured for this product"

**Solution**: Set price for the product in specific bar:
```bash
POST /api/pricing/{barId}/{productId}
{
  "sellingPrice": 500.00,
  "costPrice": 300.00
}
```

## 📚 Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8.0
- **ORM**: Hibernate/JPA
- **Frontend**: Thymeleaf, Bootstrap 5
- **Transaction**: Spring @Transactional
- **Build**: Maven

## 🤝 Contributing

This is a POC project. For production use:
1. Add authentication/authorization
2. Implement role-based access
3. Add barcode scanning integration
4. Mobile app for data entry
5. Real-time notifications

## 📄 License

This project is for educational/commercial use in the Indian bar industry.

## 👥 Support

For issues or questions:
- Check logs in `logs/` directory
- Review database state with status queries
- Contact: support@barinventory.com

---

**Built with ❤️ for Indian Bar Owners to prevent theft and track sales accurately**
