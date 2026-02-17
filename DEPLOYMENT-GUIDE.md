# Production Deployment Guide

Complete guide for deploying Liquor Inventory System in production environment.

## 🎯 Production Checklist

### Pre-Deployment
- [ ] Database server configured and hardened
- [ ] Application server (Tomcat/standalone) ready
- [ ] SSL certificates obtained
- [ ] Backup strategy defined
- [ ] Monitoring tools configured
- [ ] Security review completed
- [ ] Load testing performed

### Environment Setup
- [ ] Production database created
- [ ] Application user credentials set
- [ ] Environment variables configured
- [ ] Firewall rules applied
- [ ] Reverse proxy configured (nginx/Apache)

## 🗄️ Database Production Setup

### 1. MySQL Production Configuration

```sql
-- Create production database
CREATE DATABASE liquor_inventory_prod 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- Create application user with limited privileges
CREATE USER 'liquorapp_prod'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD_HERE';

-- Grant only necessary privileges
GRANT SELECT, INSERT, UPDATE ON liquor_inventory_prod.* TO 'liquorapp_prod'@'localhost';

-- NO DELETE privilege in production to preserve audit trail
FLUSH PRIVILEGES;

-- Enable binary logging for disaster recovery
SET GLOBAL binlog_format = 'ROW';
SET GLOBAL expire_logs_days = 7;
```

### 2. Database Performance Tuning

```sql
-- Add indexes for better performance
ALTER TABLE inventory_sessions ADD INDEX idx_bar_status (bar_id, status);
ALTER TABLE inventory_sessions ADD INDEX idx_start_time (session_start_time);
ALTER TABLE sales_records ADD INDEX idx_session_product (session_id, product_id);
ALTER TABLE stockroom_inventory ADD INDEX idx_session (session_id);
ALTER TABLE distribution_records ADD INDEX idx_session (session_id);
ALTER TABLE well_inventory ADD INDEX idx_session (session_id);

-- Analyze tables for optimization
ANALYZE TABLE bars, products, inventory_sessions, sales_records;
```

### 3. Backup Strategy

```bash
#!/bin/bash
# Daily backup script - save as /opt/scripts/backup_liquor_db.sh

BACKUP_DIR="/var/backups/liquor_inventory"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="liquor_inventory_prod"
DB_USER="backup_user"
DB_PASS="backup_password"

# Create backup directory if not exists
mkdir -p $BACKUP_DIR

# Perform backup
mysqldump -u$DB_USER -p$DB_PASS \
  --single-transaction \
  --routines \
  --triggers \
  $DB_NAME | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# Keep only last 30 days of backups
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete

echo "Backup completed: backup_$DATE.sql.gz"
```

**Setup cron job:**
```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * /opt/scripts/backup_liquor_db.sh >> /var/log/liquor_backup.log 2>&1
```

## 🚀 Application Deployment

### 1. Production Configuration

Create `application-prod.properties`:

```properties
# Server Configuration
server.port=8080
server.tomcat.threads.max=200
server.tomcat.accept-count=100

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/liquor_inventory_prod?useSSL=true&requireSSL=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Logging
logging.level.root=WARN
logging.level.com.barinventory=INFO
logging.file.name=/var/log/liquor-inventory/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Security Headers
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true

# Actuator for monitoring
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=when-authorized
```

### 2. Build Production JAR

```bash
# Set production profile
export SPRING_PROFILES_ACTIVE=prod

# Build with Maven
mvn clean package -DskipTests

# Verify JAR created
ls -lh target/liquor-inventory-system-1.0.0.jar
```

### 3. Systemd Service Configuration

Create `/etc/systemd/system/liquor-inventory.service`:

```ini
[Unit]
Description=Liquor Inventory Management System
After=syslog.target network.target mysql.service

[Service]
User=liquorapp
WorkingDirectory=/opt/liquor-inventory
ExecStart=/usr/bin/java \
    -Xms512m \
    -Xmx2048m \
    -XX:+UseG1GC \
    -Dspring.profiles.active=prod \
    -Dserver.port=8080 \
    -jar /opt/liquor-inventory/liquor-inventory-system-1.0.0.jar

SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Deploy and start:**
```bash
# Copy JAR to deployment location
sudo mkdir -p /opt/liquor-inventory
sudo cp target/liquor-inventory-system-1.0.0.jar /opt/liquor-inventory/

# Create application user
sudo useradd -r -s /bin/false liquorapp
sudo chown -R liquorapp:liquorapp /opt/liquor-inventory

# Create log directory
sudo mkdir -p /var/log/liquor-inventory
sudo chown liquorapp:liquorapp /var/log/liquor-inventory

# Enable and start service
sudo systemctl enable liquor-inventory
sudo systemctl start liquor-inventory

# Check status
sudo systemctl status liquor-inventory

# View logs
sudo journalctl -u liquor-inventory -f
```

## 🔒 Security Hardening

### 1. SSL/TLS Configuration

**Option A: Nginx Reverse Proxy (Recommended)**

```nginx
# /etc/nginx/sites-available/liquor-inventory

upstream liquor_app {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name inventory.yourbar.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name inventory.yourbar.com;

    ssl_certificate /etc/letsencrypt/live/inventory.yourbar.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/inventory.yourbar.com/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    location / {
        proxy_pass http://liquor_app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static resources caching
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        proxy_pass http://liquor_app;
        expires 1M;
        add_header Cache-Control "public, immutable";
    }
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/liquor-inventory /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 2. Firewall Configuration

```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable

# Block direct access to application port
sudo ufw deny 8080/tcp
```

### 3. Database Security

```sql
-- Restrict access to localhost only
CREATE USER 'liquorapp_prod'@'localhost' IDENTIFIED BY 'strong_password';

-- Enable SSL for database connections
ALTER USER 'liquorapp_prod'@'localhost' REQUIRE SSL;

-- Set password expiry
ALTER USER 'liquorapp_prod'@'localhost' PASSWORD EXPIRE INTERVAL 90 DAY;
```

## 📊 Monitoring & Alerts

### 1. Application Monitoring

**Install Prometheus + Grafana:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'liquor-inventory'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 2. Database Monitoring

```sql
-- Monitor session status
CREATE VIEW v_session_summary AS
SELECT 
    DATE(session_start_time) as date,
    status,
    COUNT(*) as count,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count,
    SUM(CASE WHEN status = 'ROLLED_BACK' THEN 1 ELSE 0 END) as failed_count
FROM inventory_sessions
GROUP BY DATE(session_start_time), status;

-- Daily revenue tracking
CREATE VIEW v_daily_revenue AS
SELECT 
    DATE(i.session_start_time) as date,
    b.bar_name,
    SUM(s.total_revenue) as total_revenue,
    SUM(s.profit) as total_profit,
    COUNT(DISTINCT i.session_id) as sessions_count
FROM sales_records s
JOIN inventory_sessions i ON s.session_id = i.session_id
JOIN bars b ON i.bar_id = b.bar_id
WHERE i.status = 'COMPLETED'
GROUP BY DATE(i.session_start_time), b.bar_name;
```

### 3. Alert Scripts

```bash
#!/bin/bash
# Check for failed sessions - /opt/scripts/check_failed_sessions.sh

FAILED_COUNT=$(mysql -u monitor -p'password' -D liquor_inventory_prod -se \
    "SELECT COUNT(*) FROM inventory_sessions 
     WHERE status = 'ROLLED_BACK' AND DATE(session_start_time) = CURDATE()")

if [ $FAILED_COUNT -gt 0 ]; then
    # Send alert email
    echo "Alert: $FAILED_COUNT failed sessions today" | \
    mail -s "Liquor Inventory Alert" admin@yourbar.com
fi
```

## 🔄 Upgrade Strategy

### Zero-Downtime Deployment

```bash
#!/bin/bash
# Deployment script - deploy.sh

APP_NAME="liquor-inventory"
NEW_VERSION=$1
OLD_JAR="/opt/$APP_NAME/current.jar"
NEW_JAR="/opt/$APP_NAME/$APP_NAME-$NEW_VERSION.jar"

# 1. Upload new JAR
echo "Uploading new version..."
scp target/$APP_NAME-$NEW_VERSION.jar server:/opt/$APP_NAME/

# 2. Health check on old version
curl -f http://localhost:8080/actuator/health || exit 1

# 3. Stop application
sudo systemctl stop liquor-inventory

# 4. Backup database
/opt/scripts/backup_liquor_db.sh

# 5. Run database migrations if any
# flyway migrate

# 6. Switch to new JAR
sudo mv $NEW_JAR $OLD_JAR

# 7. Start application
sudo systemctl start liquor-inventory

# 8. Wait for startup
sleep 10

# 9. Health check
for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health; then
        echo "Deployment successful!"
        exit 0
    fi
    sleep 2
done

# Rollback if health check fails
echo "Deployment failed! Rolling back..."
sudo systemctl stop liquor-inventory
sudo cp /opt/$APP_NAME/backup/previous.jar $OLD_JAR
sudo systemctl start liquor-inventory
exit 1
```

## 📈 Performance Tuning

### 1. JVM Tuning

```bash
# Update systemd service
-Xms1g                          # Initial heap
-Xmx4g                          # Maximum heap
-XX:+UseG1GC                    # G1 Garbage Collector
-XX:MaxGCPauseMillis=200        # Target pause time
-XX:+HeapDumpOnOutOfMemoryError # Dump on OOM
-XX:HeapDumpPath=/var/log/liquor-inventory/
```

### 2. Database Query Optimization

```sql
-- Add composite indexes for frequent queries
CREATE INDEX idx_session_bar_date ON inventory_sessions(bar_id, session_start_time, status);
CREATE INDEX idx_sales_session_product ON sales_records(session_id, product_id);

-- Partition large tables by date
ALTER TABLE inventory_sessions
PARTITION BY RANGE (YEAR(session_start_time)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027)
);
```

## 🆘 Disaster Recovery

### Recovery Procedure

```bash
# 1. Stop application
sudo systemctl stop liquor-inventory

# 2. Restore database from backup
zcat /var/backups/liquor_inventory/backup_20240211_020000.sql.gz | \
mysql -u root -p liquor_inventory_prod

# 3. Verify data integrity
mysql -u root -p liquor_inventory_prod -e "
    SELECT COUNT(*) FROM bars;
    SELECT COUNT(*) FROM inventory_sessions;
    SELECT MAX(session_start_time) FROM inventory_sessions;
"

# 4. Start application
sudo systemctl start liquor-inventory

# 5. Verify application health
curl http://localhost:8080/actuator/health
```

## 📋 Production Checklist

### Daily Operations
- [ ] Check application logs for errors
- [ ] Verify backup completion
- [ ] Monitor disk space usage
- [ ] Check failed sessions count
- [ ] Review revenue reports

### Weekly Operations
- [ ] Review application performance metrics
- [ ] Check database slow query log
- [ ] Verify SSL certificate expiry
- [ ] Test backup restoration
- [ ] Review security logs

### Monthly Operations
- [ ] Update dependencies and security patches
- [ ] Database optimization (ANALYZE, OPTIMIZE)
- [ ] Archive old data
- [ ] Capacity planning review
- [ ] Security audit

---

**Production Support:** support@yourbar.com
**Emergency Contact:** +91-XXXX-XXXX
