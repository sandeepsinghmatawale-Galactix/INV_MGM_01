# Quick Start Guide - Liquor Inventory System

## ⚡ Get Started in 5 Minutes

### Step 1: Prerequisites Check
```bash
# Check Java version (need 17+)
java -version

# Check Maven
mvn -version

# Check MySQL
mysql --version
```

### Step 2: Database Setup
```bash
# Login to MySQL
mysql -u root -p

# Run these commands:
CREATE DATABASE liquor_inventory_db;
exit;
```

### Step 3: Run the Application
```bash
# Navigate to project directory
cd liquor-inventory-system

# Build and run
mvn spring-boot:run
```

### Step 4: Access Application
Open browser: **http://localhost:8080**

### Step 5: Load Sample Data (Optional)
```bash
# In another terminal
mysql -u root -p liquor_inventory_db < sample-data.sql
```

## 🎯 Quick Test Workflow

### Using Web Interface:

1. **Home Page** → Click "Add Bar"
   - Name: "Test Bar"
   - Location: "Test Location"
   - Save

2. **Add Products** → Navigate to Products → Add New
   - Name: "Test Whisky"
   - Category: "Whisky"
   - Save

3. **Set Price** → Go to bar → Manage Pricing
   - Select product
   - Selling Price: 500
   - Cost Price: 350
   - Save

4. **Start Session** → Manage Sessions → New Session
   - Shift: Evening
   - Start

5. **Enter Stockroom Data**
   - Opening: 100
   - Received: 50
   - Closing: 120
   - Save (Auto-calculates: 30 transferred)

6. **Create Distribution**
   - Click "Create Distribution"
   - Verify: 30 bottles pending allocation

7. **Allocate to Wells**
   - BAR_1: Received: 20, Opening: 10, Closing: 5
   - BAR_2: Received: 10, Opening: 5, Closing: 8
   - Save

8. **Commit Session**
   - Click "Commit"
   - ✅ Success! View sales report

## 🧪 Quick API Test

```bash
# 1. Initialize session
curl -X POST 'http://localhost:8080/api/sessions/initialize?barId=1&shiftType=EVENING'
# Note the sessionId from response

# 2. Enter stockroom (use your sessionId)
curl -X POST http://localhost:8080/api/sessions/1/stockroom \
  -H "Content-Type: application/json" \
  -d '[{"product":{"productId":1},"openingStock":100,"receivedStock":50,"closingStock":120}]'

# 3. Create distribution
curl -X POST http://localhost:8080/api/sessions/1/distribution/create

# 4. Allocate to wells
curl -X POST http://localhost:8080/api/sessions/1/wells \
  -H "Content-Type: application/json" \
  -d '[{"product":{"productId":1},"wellName":"BAR_1","openingStock":10,"receivedFromDistribution":30,"closingStock":5}]'

# 5. Commit
curl -X POST http://localhost:8080/api/sessions/1/commit

# 6. View session
curl http://localhost:8080/api/sessions/1
```

## 📂 Project Structure

```
liquor-inventory-system/
├── pom.xml                          # Maven dependencies
├── src/main/
│   ├── java/com/barinventory/
│   │   ├── entity/                  # Database entities
│   │   ├── repository/              # Data access
│   │   ├── service/                 # Business logic
│   │   ├── controller/              # REST & Web controllers
│   │   └── LiquorInventoryApplication.java
│   └── resources/
│       ├── application.properties   # Configuration
│       └── templates/               # Thymeleaf views
├── README.md                        # Full documentation
├── API-TESTING-GUIDE.md            # API examples
├── DEPLOYMENT-GUIDE.md             # Production setup
├── ARCHITECTURE.md                  # System design
└── sample-data.sql                  # Test data
```

## 🔍 Key Features

### ✅ What It Does
- **3-Stage Tracking**: Stockroom → Distribution → Wells
- **Theft Prevention**: Distribution checkpoint prevents stock vanishing
- **Atomic Transactions**: All-or-nothing commits
- **Multi-Bar Support**: Each bar independent with own pricing
- **Sales Calculation**: Auto-generates revenue from consumed stock

### 🔒 Business Rules
1. Opening + Received - Closing = Transferred ✓
2. Stockroom Transferred = Distribution Total ✓
3. Distribution Allocated = Wells Received ✓
4. No Unallocated Stock Allowed ✓
5. Consumed × Price = Revenue ✓

## 🛠️ Common Issues

### "Port 8080 already in use"
```bash
# Change port in application.properties
server.port=8081
```

### "Database connection failed"
```bash
# Check MySQL is running
sudo systemctl status mysql

# Verify credentials in application.properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### "Price not found" error
```bash
# Set price first
curl -X POST http://localhost:8080/api/pricing/1/1 \
  -H "Content-Type: application/json" \
  -d '{"sellingPrice":500,"costPrice":350}'
```

## 📊 Sample Reports

### View Daily Sales
```bash
curl 'http://localhost:8080/api/reports/1/daily?date=2024-02-11'
```

### View Monthly Sales
```bash
curl 'http://localhost:8080/api/reports/1/monthly?year=2024&month=2'
```

## 🎓 Next Steps

1. **Read Full Documentation**: `README.md`
2. **API Testing**: `API-TESTING-GUIDE.md`
3. **Production Deployment**: `DEPLOYMENT-GUIDE.md`
4. **System Architecture**: `ARCHITECTURE.md`

## 💡 Pro Tips

- Always start with sample data for testing
- Use Postman for API testing (import collection)
- Check logs: `tail -f logs/application.log`
- Monitor MySQL: `SHOW PROCESSLIST;`
- Backup before production: `mysqldump`

## 🆘 Support

**Questions?** Check the documentation files:
- Technical issues → DEPLOYMENT-GUIDE.md
- API usage → API-TESTING-GUIDE.md
- System design → ARCHITECTURE.md

**Need Help?** 
- GitHub Issues: [Create issue]
- Email: support@barinventory.com

---

**Happy Coding! 🚀**

Built for Indian Bar Industry with ❤️
