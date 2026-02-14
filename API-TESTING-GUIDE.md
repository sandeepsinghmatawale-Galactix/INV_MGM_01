# API Testing Guide - Liquor Inventory System

Complete guide to test all features using cURL or Postman.

## Prerequisites

- Application running on `http://localhost:8080`
- MySQL database running with sample data loaded
- Tools: cURL, Postman, or any REST client

## 1. Bar Management APIs

### Create a Bar
```bash
curl -X POST http://localhost:8080/api/bars \
  -H "Content-Type: application/json" \
  -d '{
    "barName": "Test Bar",
    "location": "Test Location, Hyderabad",
    "contactNumber": "9999999999",
    "ownerName": "Test Owner",
    "active": true
  }'
```

### Get All Bars
```bash
curl http://localhost:8080/api/bars
```

### Get Specific Bar
```bash
curl http://localhost:8080/api/bars/1
```

## 2. Product Management APIs

### Create a Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Test Whisky",
    "category": "Whisky",
    "brand": "Test Brand",
    "volumeML": 750.00,
    "unit": "BOTTLE",
    "active": true
  }'
```

### Get All Products
```bash
curl http://localhost:8080/api/products
```

## 3. Pricing APIs

### Set Price for Product in Bar
```bash
curl -X POST http://localhost:8080/api/pricing/1/1 \
  -H "Content-Type: application/json" \
  -d '{
    "sellingPrice": 500.00,
    "costPrice": 350.00,
    "active": true
  }'
```

### Get All Prices for a Bar
```bash
curl http://localhost:8080/api/pricing/1
```

## 4. Complete Inventory Session Workflow

### Step 1: Initialize Session
```bash
curl -X POST 'http://localhost:8080/api/sessions/initialize?barId=1&shiftType=EVENING&notes=Test%20Session' \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "sessionId": 4,
  "bar": {...},
  "sessionStartTime": "2024-02-11T20:30:00",
  "status": "IN_PROGRESS",
  "shiftType": "EVENING"
}
```

**Save the sessionId for next steps!**

### Step 2: Enter Stockroom Inventory
```bash
curl -X POST http://localhost:8080/api/sessions/4/stockroom \
  -H "Content-Type: application/json" \
  -d '[
    {
      "product": {"productId": 1},
      "openingStock": 100,
      "receivedStock": 50,
      "closingStock": 120,
      "remarks": "Physical count verified"
    },
    {
      "product": {"productId": 2},
      "openingStock": 20,
      "receivedStock": 10,
      "closingStock": 25,
      "remarks": "All good"
    },
    {
      "product": {"productId": 11},
      "openingStock": 200,
      "receivedStock": 100,
      "closingStock": 250,
      "remarks": "Beer stock checked"
    }
  ]'
```

**Calculations Done Automatically:**
- Product 1: 100 + 50 - 120 = **30 transferred**
- Product 2: 20 + 10 - 25 = **5 transferred**
- Product 11: 200 + 100 - 250 = **50 transferred**

### Step 3: Create Distribution Records
```bash
curl -X POST http://localhost:8080/api/sessions/4/distribution/create \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "message": "Distribution records created"
}
```

This creates distribution entries matching stockroom transferred quantities.

### Step 4: Allocate to Wells
```bash
curl -X POST http://localhost:8080/api/sessions/4/wells \
  -H "Content-Type: application/json" \
  -d '[
    {
      "product": {"productId": 1},
      "wellName": "BAR_1",
      "openingStock": 10,
      "receivedFromDistribution": 20,
      "closingStock": 5,
      "remarks": "Main bar counter"
    },
    {
      "product": {"productId": 1},
      "wellName": "BAR_2",
      "openingStock": 5,
      "receivedFromDistribution": 10,
      "closingStock": 8,
      "remarks": "Service bar"
    },
    {
      "product": {"productId": 2},
      "wellName": "BAR_1",
      "openingStock": 3,
      "receivedFromDistribution": 5,
      "closingStock": 2,
      "remarks": "Premium bar"
    },
    {
      "product": {"productId": 11},
      "wellName": "BAR_1",
      "openingStock": 30,
      "receivedFromDistribution": 30,
      "closingStock": 10,
      "remarks": "Beer counter 1"
    },
    {
      "product": {"productId": 11},
      "wellName": "BAR_2",
      "openingStock": 20,
      "receivedFromDistribution": 20,
      "closingStock": 15,
      "remarks": "Beer counter 2"
    }
  ]'
```

**Validation Check:**
- Product 1: 20 + 10 = 30 ✓ (matches distribution)
- Product 2: 5 = 5 ✓
- Product 11: 30 + 20 = 50 ✓

**Consumption Calculated:**
- Product 1, BAR_1: 10 + 20 - 5 = **25 sold**
- Product 1, BAR_2: 5 + 10 - 8 = **7 sold**
- Product 2, BAR_1: 3 + 5 - 2 = **6 sold**
- Product 11, BAR_1: 30 + 30 - 10 = **50 sold**
- Product 11, BAR_2: 20 + 20 - 15 = **25 sold**

### Step 5: Commit Session (Final Validation & Sales Generation)
```bash
curl -X POST http://localhost:8080/api/sessions/4/commit \
  -H "Content-Type: application/json"
```

**Successful Response:**
```json
{
  "message": "Session committed successfully"
}
```

**What Happens:**
1. ✅ Validates stockroom → distribution
2. ✅ Validates distribution → wells
3. ✅ Checks no unallocated stock
4. ✅ Calculates total consumed per product
5. ✅ Fetches bar-specific prices
6. ✅ Generates sales records:
   - Product 1: (25+7) × ₹500 = ₹16,000
   - Product 2: 6 × ₹3,500 = ₹21,000
   - Product 11: (50+25) × ₹150 = ₹11,250
7. ✅ Sets status to COMPLETED

### Testing Validation Failure

**Scenario: Unallocated Stock**

```bash
# Step 1-3: Same as above

# Step 4: Allocate LESS than distributed (intentional error)
curl -X POST http://localhost:8080/api/sessions/5/wells \
  -H "Content-Type: application/json" \
  -d '[
    {
      "product": {"productId": 1},
      "wellName": "BAR_1",
      "openingStock": 10,
      "receivedFromDistribution": 15,
      "closingStock": 5
    }
  ]'
```

Here we allocated only 15 bottles but stockroom transferred 30.

```bash
# Step 5: Try to commit
curl -X POST http://localhost:8080/api/sessions/5/commit
```

**Error Response:**
```json
{
  "error": "Validation failed: Product Royal Challenge: Unallocated stock remaining (15 units)."
}
```

**Session Status → ROLLED_BACK**

## 5. Query Session Data

### Get Session Details
```bash
curl http://localhost:8080/api/sessions/4
```

**Response includes:**
- Session metadata
- All stockroom records
- All distribution records
- All well inventories
- All sales records

### Get All Sessions for a Bar
```bash
curl http://localhost:8080/api/sessions/bar/1
```

### Get Sessions by Date Range
```bash
curl 'http://localhost:8080/api/sessions/bar/1/daterange?startDate=2024-02-10T00:00:00&endDate=2024-02-12T23:59:59'
```

## 6. Reporting APIs

### Get Daily Sales Report
```bash
curl 'http://localhost:8080/api/reports/1/daily?date=2024-02-11'
```

**Response:**
```json
{
  "date": "2024-02-11",
  "totalRevenue": 48250.00,
  "totalCost": 35500.00,
  "totalProfit": 12750.00,
  "salesRecords": [...]
}
```

### Get Weekly Sales
```bash
curl 'http://localhost:8080/api/reports/1/weekly?startDate=2024-02-05'
```

### Get Monthly Sales
```bash
curl 'http://localhost:8080/api/reports/1/monthly?year=2024&month=2'
```

**Response includes:**
```json
{
  "year": 2024,
  "month": 2,
  "totalRevenue": 145000.00,
  "productWiseSales": {
    "Royal Challenge": 48000.00,
    "Black Label": 63000.00,
    "Kingfisher Strong": 34000.00
  }
}
```

### Get Product-Wise Summary
```bash
curl 'http://localhost:8080/api/reports/1/product-summary?startDate=2024-02-01T00:00:00&endDate=2024-02-28T23:59:59'
```

## 7. Testing Edge Cases

### Case 1: Stockroom-Distribution Mismatch
```json
// Stockroom says 30 transferred
// But create distribution with 25
// Result: Validation fails at commit
```

### Case 2: Distribution-Wells Mismatch
```json
// Distribution has 30 bottles
// Wells receive only 28
// Result: Validation fails - "Unallocated stock remaining (2 units)"
```

### Case 3: Negative Stock
```json
// Opening: 10, Received: 5, Closing: 20
// Transferred: 10 + 5 - 20 = -5
// Result: Business logic error (more closing than available)
```

### Case 4: Decimal Quantities
```json
{
  "openingStock": 10.5,
  "receivedStock": 5.25,
  "closingStock": 12.75
  // Transferred: 10.5 + 5.25 - 12.75 = 3.0
}
```

## 8. Rollback Testing

### Manual Rollback
```bash
curl -X POST 'http://localhost:8080/api/sessions/4/rollback?reason=Manager%20decision' \
  -H "Content-Type: application/json"
```

**Effect:**
- Status → ROLLED_BACK
- validationErrors field updated
- No sales records generated
- Data preserved for audit

## 9. Complete Test Sequence

```bash
# 1. Create Bar
BAR_ID=$(curl -s -X POST http://localhost:8080/api/bars -H "Content-Type: application/json" -d '{"barName":"Test Bar","location":"Test","contactNumber":"9999999999","ownerName":"Test","active":true}' | jq -r '.barId')

# 2. Create Product
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{"productName":"Test Product","category":"Whisky","brand":"Test","volumeML":750,"unit":"BOTTLE","active":true}' | jq -r '.productId')

# 3. Set Price
curl -s -X POST http://localhost:8080/api/pricing/$BAR_ID/$PRODUCT_ID -H "Content-Type: application/json" -d '{"sellingPrice":500,"costPrice":350,"active":true}'

# 4. Initialize Session
SESSION_ID=$(curl -s -X POST "http://localhost:8080/api/sessions/initialize?barId=$BAR_ID&shiftType=EVENING" | jq -r '.sessionId')

# 5. Stockroom
curl -s -X POST http://localhost:8080/api/sessions/$SESSION_ID/stockroom -H "Content-Type: application/json" -d "[{\"product\":{\"productId\":$PRODUCT_ID},\"openingStock\":100,\"receivedStock\":50,\"closingStock\":120}]"

# 6. Distribution
curl -s -X POST http://localhost:8080/api/sessions/$SESSION_ID/distribution/create

# 7. Wells
curl -s -X POST http://localhost:8080/api/sessions/$SESSION_ID/wells -H "Content-Type: application/json" -d "[{\"product\":{\"productId\":$PRODUCT_ID},\"wellName\":\"BAR_1\",\"openingStock\":10,\"receivedFromDistribution\":30,\"closingStock\":5}]"

# 8. Commit
curl -s -X POST http://localhost:8080/api/sessions/$SESSION_ID/commit

# 9. Verify
curl -s http://localhost:8080/api/sessions/$SESSION_ID | jq '.status'
# Should output: "COMPLETED"
```

## 10. Performance Testing

### Bulk Session Creation
```bash
for i in {1..10}
do
  SESSION=$(curl -s -X POST "http://localhost:8080/api/sessions/initialize?barId=1&shiftType=TEST")
  SESSION_ID=$(echo $SESSION | jq -r '.sessionId')
  echo "Created session: $SESSION_ID"
done
```

## Expected Outcomes

### Success Scenario
✅ All validations pass
✅ Sales records created
✅ Revenue calculated correctly
✅ Status = COMPLETED
✅ Profit = Revenue - Cost

### Failure Scenario
❌ Validation fails
❌ Error message logged
❌ Status = ROLLED_BACK
❌ No sales records created
❌ Data preserved for review

## Troubleshooting

### Error: "Session not in progress"
**Cause**: Trying to update a completed/rolled-back session
**Fix**: Initialize a new session

### Error: "Price not configured"
**Cause**: Product price not set for the specific bar
**Fix**: Use pricing API to set price first

### Error: "Bar not found"
**Cause**: Invalid barId
**Fix**: Use `/api/bars` to get valid IDs

---

**Happy Testing! 🚀**

For any issues, check application logs or database state directly.
