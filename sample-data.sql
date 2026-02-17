-- Sample Data for Liquor Inventory Management System
-- Run this after application starts and creates tables

USE liquor_inventory_db;

-- Insert Sample Bars
INSERT INTO bars (bar_name, location, contact_number, owner_name, active) VALUES
('The King''s Tavern', 'Banjara Hills, Hyderabad', '9876543210', 'Rajesh Kumar', true),
('Blue Moon Bar', 'Jubilee Hills, Hyderabad', '9876543211', 'Suresh Reddy', true),
('Sunset Lounge', 'Gachibowli, Hyderabad', '9876543212', 'Prakash Sharma', true);

-- Insert Sample Products
INSERT INTO products (product_name, category, brand, volume_ml, unit, active) VALUES
-- Whisky
('Royal Challenge', 'Whisky', 'Royal Challenge', 750.00, 'BOTTLE', true),
('Black Label', 'Whisky', 'Johnnie Walker', 750.00, 'BOTTLE', true),
('Blenders Pride', 'Whisky', 'Blenders Pride', 750.00, 'BOTTLE', true),
('Royal Stag', 'Whisky', 'Royal Stag', 750.00, 'BOTTLE', true),

-- Vodka
('Absolut Vodka', 'Vodka', 'Absolut', 750.00, 'BOTTLE', true),
('Smirnoff', 'Vodka', 'Smirnoff', 750.00, 'BOTTLE', true),
('Magic Moments', 'Vodka', 'Magic Moments', 750.00, 'BOTTLE', true),

-- Rum
('Bacardi White', 'Rum', 'Bacardi', 750.00, 'BOTTLE', true),
('Old Monk', 'Rum', 'Old Monk', 750.00, 'BOTTLE', true),
('Captain Morgan', 'Rum', 'Captain Morgan', 750.00, 'BOTTLE', true),

-- Beer
('Kingfisher Strong', 'Beer', 'Kingfisher', 650.00, 'BOTTLE', true),
('Budweiser', 'Beer', 'Budweiser', 650.00, 'BOTTLE', true),
('Corona', 'Beer', 'Corona', 330.00, 'BOTTLE', true),

-- Wine
('Sula Shiraz', 'Wine', 'Sula', 750.00, 'BOTTLE', true),
('Fratelli Sangiovese', 'Wine', 'Fratelli', 750.00, 'BOTTLE', true);

-- Set Bar-Specific Pricing for Bar 1 (The King's Tavern)
INSERT INTO bar_product_prices (bar_id, product_id, selling_price, cost_price, active) VALUES
(1, 1, 500.00, 350.00, true),  -- Royal Challenge
(1, 2, 3500.00, 2800.00, true), -- Black Label
(1, 3, 450.00, 320.00, true),  -- Blenders Pride
(1, 4, 400.00, 280.00, true),  -- Royal Stag
(1, 5, 1200.00, 900.00, true), -- Absolut
(1, 6, 800.00, 600.00, true),  -- Smirnoff
(1, 7, 350.00, 250.00, true),  -- Magic Moments
(1, 8, 900.00, 700.00, true),  -- Bacardi
(1, 9, 300.00, 200.00, true),  -- Old Monk
(1, 10, 1000.00, 800.00, true), -- Captain Morgan
(1, 11, 150.00, 100.00, true),  -- Kingfisher
(1, 12, 200.00, 150.00, true),  -- Budweiser
(1, 13, 250.00, 180.00, true),  -- Corona
(1, 14, 800.00, 600.00, true),  -- Sula
(1, 15, 1200.00, 950.00, true); -- Fratelli

-- Set Bar-Specific Pricing for Bar 2 (Blue Moon Bar) - Higher prices
INSERT INTO bar_product_prices (bar_id, product_id, selling_price, cost_price, active) VALUES
(2, 1, 550.00, 350.00, true),
(2, 2, 3800.00, 2800.00, true),
(2, 3, 480.00, 320.00, true),
(2, 4, 420.00, 280.00, true),
(2, 5, 1300.00, 900.00, true),
(2, 6, 850.00, 600.00, true),
(2, 11, 180.00, 100.00, true),
(2, 12, 220.00, 150.00, true);

-- Set Bar-Specific Pricing for Bar 3 (Sunset Lounge) - Premium pricing
INSERT INTO bar_product_prices (bar_id, product_id, selling_price, cost_price, active) VALUES
(3, 2, 4000.00, 2800.00, true),
(3, 5, 1400.00, 900.00, true),
(3, 8, 1100.00, 700.00, true),
(3, 13, 300.00, 180.00, true),
(3, 14, 900.00, 600.00, true),
(3, 15, 1400.00, 950.00, true);

-- Sample Complete Session (COMPLETED)
-- Bar 1, Session 1
INSERT INTO inventory_sessions (bar_id, session_start_time, session_end_time, status, shift_type, notes)
VALUES (1, '2024-02-10 18:00:00', '2024-02-11 02:00:00', 'COMPLETED', 'EVENING', 'Saturday night shift');

SET @session1 = LAST_INSERT_ID();

-- Stockroom Inventory for Session 1
INSERT INTO stockroom_inventory (session_id, product_id, opening_stock, received_stock, closing_stock, transferred_out)
VALUES
(@session1, 1, 100, 50, 120, 30),  -- Royal Challenge: 100+50-120=30 transferred
(@session1, 2, 20, 10, 25, 5),     -- Black Label: 20+10-25=5 transferred
(@session1, 11, 200, 100, 250, 50); -- Kingfisher: 200+100-250=50 transferred

-- Distribution Records for Session 1
INSERT INTO distribution_records (session_id, product_id, quantity_from_stockroom, total_allocated, unallocated, status)
VALUES
(@session1, 1, 30, 30, 0, 'COMPLETED'),
(@session1, 2, 5, 5, 0, 'COMPLETED'),
(@session1, 11, 50, 50, 0, 'COMPLETED');

-- Well Inventory for Session 1
INSERT INTO well_inventory (session_id, product_id, well_name, opening_stock, received_from_distribution, closing_stock, consumed)
VALUES
-- Royal Challenge distributed to 2 wells
(@session1, 1, 'BAR_1', 10, 20, 5, 25),   -- 10+20-5=25 consumed
(@session1, 1, 'BAR_2', 5, 10, 8, 7),     -- 5+10-8=7 consumed

-- Black Label to main bar only
(@session1, 2, 'BAR_1', 3, 5, 2, 6),      -- 3+5-2=6 consumed

-- Kingfisher to both bars
(@session1, 11, 'BAR_1', 30, 30, 10, 50), -- 30+30-10=50 consumed
(@session1, 11, 'BAR_2', 20, 20, 15, 25); -- 20+20-15=25 consumed

-- Sales Records for Session 1
INSERT INTO sales_records (session_id, product_id, quantity_sold, selling_price_per_unit, total_revenue, cost_price_per_unit, total_cost, profit)
VALUES
(@session1, 1, 32, 500.00, 16000.00, 350.00, 11200.00, 4800.00),   -- Royal Challenge: 25+7=32
(@session1, 2, 6, 3500.00, 21000.00, 2800.00, 16800.00, 4200.00),  -- Black Label: 6
(@session1, 11, 75, 150.00, 11250.00, 100.00, 7500.00, 3750.00);   -- Kingfisher: 50+25=75

-- Sample In-Progress Session
-- Bar 1, Session 2
INSERT INTO inventory_sessions (bar_id, session_start_time, status, shift_type, notes)
VALUES (1, '2024-02-11 18:00:00', 'IN_PROGRESS', 'EVENING', 'Sunday evening shift');

-- Another Completed Session for Bar 2
INSERT INTO inventory_sessions (bar_id, session_start_time, session_end_time, status, shift_type, notes)
VALUES (2, '2024-02-10 19:00:00', '2024-02-11 03:00:00', 'COMPLETED', 'EVENING', 'Weekend special');

SET @session3 = LAST_INSERT_ID();

INSERT INTO stockroom_inventory (session_id, product_id, opening_stock, received_stock, closing_stock, transferred_out)
VALUES
(@session3, 1, 80, 20, 90, 10),
(@session3, 11, 150, 50, 180, 20);

INSERT INTO distribution_records (session_id, product_id, quantity_from_stockroom, total_allocated, unallocated, status)
VALUES
(@session3, 1, 10, 10, 0, 'COMPLETED'),
(@session3, 11, 20, 20, 0, 'COMPLETED');

INSERT INTO well_inventory (session_id, product_id, well_name, opening_stock, received_from_distribution, closing_stock, consumed)
VALUES
(@session3, 1, 'MAIN_BAR', 5, 10, 3, 12),
(@session3, 11, 'MAIN_BAR', 15, 20, 10, 25);

INSERT INTO sales_records (session_id, product_id, quantity_sold, selling_price_per_unit, total_revenue, cost_price_per_unit, total_cost, profit)
VALUES
(@session3, 1, 12, 550.00, 6600.00, 350.00, 4200.00, 2400.00),
(@session3, 11, 25, 180.00, 4500.00, 100.00, 2500.00, 2000.00);

-- Verify Data
SELECT 'Bars Created:' as Info, COUNT(*) as Count FROM bars;
SELECT 'Products Created:' as Info, COUNT(*) as Count FROM products;
SELECT 'Prices Configured:' as Info, COUNT(*) as Count FROM bar_product_prices;
SELECT 'Sessions Created:' as Info, COUNT(*) as Count FROM inventory_sessions;
SELECT 'Completed Sessions:' as Info, COUNT(*) as Count FROM inventory_sessions WHERE status = 'COMPLETED';

-- Show sample sales data
SELECT 
    b.bar_name,
    p.product_name,
    s.quantity_sold,
    s.selling_price_per_unit,
    s.total_revenue,
    s.profit
FROM sales_records s
JOIN inventory_sessions i ON s.session_id = i.session_id
JOIN bars b ON i.bar_id = b.bar_id
JOIN products p ON s.product_id = p.product_id
ORDER BY s.total_revenue DESC;

-- Show validation example
SELECT 
    'Stockroom → Distribution Validation' as Check_Type,
    si.product_id,
    p.product_name,
    si.transferred_out as Stockroom_Out,
    dr.quantity_from_stockroom as Distribution_In,
    CASE 
        WHEN si.transferred_out = dr.quantity_from_stockroom THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END as Status
FROM stockroom_inventory si
JOIN distribution_records dr ON si.session_id = dr.session_id AND si.product_id = dr.product_id
JOIN products p ON si.product_id = p.product_id
WHERE si.session_id = @session1;
