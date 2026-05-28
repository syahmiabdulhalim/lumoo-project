# Lumoo.my — Full Claude Code Implementation Prompt

## Project Context

You are building **lumoo.my**, a Spring Boot + Java e-commerce platform for a Gambian client.
The platform integrates with **ModemPay** (https://docs.modempay.com) — a Gambian payment gateway
that supports QMoney, Afrimoney, and Wave mobile money.

The currency is **GMD (Gambian Dalasi)**. All amounts are in the smallest unit (e.g. 450 = 4.50 GMD).

---

## Tech Stack

- **Backend:** Java 17 + Spring Boot 3.x
- **Database:** MySQL 8
- **Cache:** Redis
- **HTTP Client:** Spring WebFlux WebClient (reactive, non-blocking)
- **ORM:** Spring Data JPA + Hibernate
- **Security:** Spring Security + JWT
- **Containerisation:** Docker + Docker Compose
- **Payment Gateway:** ModemPay REST API

---

## Clean Architecture — Package by Domain (NOT by Layer)

> Use **package-by-domain** structure. This is the professional standard.
> Every file related to a feature lives in ONE folder — easier to understand,
> easier to test, easier to delete if you remove a feature.

```
lumoo/
├── src/main/java/com/lumoo/
│   │
│   ├── LumooApplication.java
│   │
│   ├── config/                              ← Configuration ONLY. No business logic.
│   │   ├── SecurityConfig.java              ← Spring Security rules
│   │   ├── ModemPayConfig.java              ← WebClient bean for ModemPay
│   │   ├── CacheConfig.java                 ← Redis cache TTL settings
│   │   ├── RateLimitConfig.java             ← Bucket4j rate limiter beans
│   │   └── EncryptionConfig.java            ← AES-256 encryptor bean
│   │
│   ├── domain/                              ← THE HEART OF YOUR APP
│   │   │
│   │   ├── product/                         ← Everything about products here
│   │   │   ├── Product.java                 ← JPA Entity
│   │   │   ├── ProductRepository.java       ← Data access
│   │   │   ├── ProductService.java          ← Business logic
│   │   │   ├── ProductController.java       ← HTTP layer (thin!)
│   │   │   └── dto/
│   │   │       ├── ProductSummaryDTO.java   ← Listing (minimal fields)
│   │   │       └── ProductDetailDTO.java    ← Single product page (full)
│   │   │
│   │   ├── order/                           ← Everything about orders here
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── OrderStatus.java             ← Enum lives with domain
│   │   │   ├── OrderRepository.java
│   │   │   ├── OrderService.java
│   │   │   ├── OrderController.java
│   │   │   └── dto/
│   │   │       ├── CheckoutRequest.java
│   │   │       └── CheckoutResponse.java
│   │   │
│   │   ├── payment/                         ← Everything about payments here
│   │   │   ├── WebhookEvent.java
│   │   │   ├── WebhookEventRepository.java
│   │   │   ├── PaymentService.java          ← ModemPay API calls ONLY
│   │   │   └── WebhookController.java
│   │   │
│   │   ├── customer/                        ← Customer rights (PDPP 2025)
│   │   │   ├── CustomerRightsController.java
│   │   │   └── CustomerRightsService.java
│   │   │
│   │   └── admin/                           ← Admin-only features
│   │       ├── AdminOrderController.java
│   │       ├── AdminProductController.java
│   │       └── AdminAuditController.java
│   │
│   ├── infrastructure/                      ← Technical concerns only
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   └── DataEncryptor.java
│   │   ├── audit/
│   │   │   ├── AuditLog.java
│   │   │   ├── AuditLogRepository.java
│   │   │   └── AuditService.java
│   │   ├── email/
│   │   │   └── EmailService.java
│   │   └── retention/
│   │       └── DataRetentionService.java    ← PDPP scheduled cleanup
│   │
│   ├── shared/                              ← Used by everyone, changes rarely
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ProductNotFoundException.java
│   │   │   ├── InsufficientStockException.java
│   │   │   └── PaymentException.java
│   │   └── response/
│   │       └── ApiResponse.java             ← Standard response wrapper
│   │
│   └── legal/
│       └── LegalController.java             ← Privacy policy, Terms (PDPP 2025)
│
├── src/main/resources/
│   ├── application.properties               ← Common config (no secrets)
│   ├── application-dev.properties           ← Dev: local DB, debug logs, no Redis
│   ├── application-prod.properties          ← Prod: real env vars, no SQL logs
│   └── db/migration/
│       ├── V1__init_schema.sql
│       ├── V2__add_security_tables.sql
│       └── V3__pdpp_compliance.sql
│
├── src/test/java/com/lumoo/
│   ├── domain/
│   │   ├── order/
│   │   │   └── OrderServiceTest.java
│   │   └── payment/
│   │       └── WebhookServiceTest.java
│   └── infrastructure/
│       └── security/
│           └── DataEncryptorTest.java
│
├── nginx/
│   └── nginx.conf
├── docker-compose.yml
├── docker-compose.dev.yml                   ← Dev: no SSL, ports exposed for IDE
├── Dockerfile
├── .env.example
├── .gitignore
└── pom.xml
```

---

## Layer Rules — Iron Rules That Must Never Be Broken

```
REQUEST FLOW (always in this direction, never backwards):

HTTP Request
    ↓
[Nginx]           ← SSL termination, static files, rate limiting
    ↓
[Controller]      ← Validate input (@Valid), check rate limit, call service
    ↓
[Service]         ← Business logic ONLY. No HTTP objects, no SQL strings.
    ↓
[Repository]      ← Database queries ONLY. No business logic.
    ↓
[Database]        ← MySQL (write) / Redis (read cache)

RULES:
  ✅ Controller  → Service    (allowed)
  ✅ Service     → Repository (allowed)
  ✅ Service     → Service    (allowed)
  ❌ Controller  → Repository (NEVER — skip the service layer)
  ❌ Repository  → Service    (NEVER — wrong direction)
  ❌ Service     → Controller (NEVER — wrong direction)
```

---

## Clean Code Rules — Must Follow In Every Class

### RULE 1 — Controller Must Be Thin (max 15 lines per method)

```java
// ✅ CORRECT — controller delegates everything
@PostMapping("/checkout")
public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
    @RequestBody @Valid CheckoutRequest request,
    HttpServletRequest httpRequest
) {
    if (!rateLimitConfig.checkoutBucketFor(getClientIp(httpRequest)).tryConsume(1)) {
        return ResponseEntity.status(429).body(ApiResponse.error("Too many requests"));
    }
    return ResponseEntity.ok(ApiResponse.ok(orderService.checkout(request, httpRequest)));
}
// Total: 5 lines. Controller does ONE thing: receive, validate, delegate, respond.
```

### RULE 2 — Never Return Entity from Controller

```java
// ❌ WRONG — exposes DB structure, version fields, internal IDs
@GetMapping("/products/{id}")
public Product getProduct(@PathVariable Long id) {
    return productRepository.findById(id).orElseThrow();
}

// ✅ CORRECT — return only what client needs
@GetMapping("/products/{id}")
public ResponseEntity<ApiResponse<ProductDetailDTO>> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
        .body(ApiResponse.ok(productService.getProductById(id)));
}
```

### RULE 3 — Use Standard ApiResponse Wrapper

```java
// shared/response/ApiResponse.java
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now().toString());
    }
}

// Every endpoint returns this — frontend always knows the shape:
// { "success": true, "message": "Success", "data": {...}, "timestamp": "..." }
// { "success": false, "message": "Insufficient stock", "data": null, "timestamp": "..." }
```

### RULE 4 — Custom Exceptions for Every Error Type

```java
// ❌ WRONG
throw new RuntimeException("Product not found: " + id);

// ✅ CORRECT — in shared/exception/
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}

// GlobalExceptionHandler maps it cleanly:
@ExceptionHandler(ProductNotFoundException.class)
public ResponseEntity<ApiResponse<?>> handle(ProductNotFoundException e) {
    return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
}
```

### RULE 5 — @Transactional on Service, NEVER Controller

```java
// ❌ WRONG
@Transactional
@PostMapping("/checkout")
public ResponseEntity<?> checkout(...) { }

// ✅ CORRECT
// Controller has no @Transactional
// Service method has @Transactional
@Transactional
public Order createPendingOrder(CheckoutRequest request) { ... }
```

### RULE 6 — No Magic Numbers or Hardcoded Values

```java
// ❌ WRONG
if (file.getSize() > 300000) { ... }
CacheControl.maxAge(60, TimeUnit.SECONDS);

// ✅ CORRECT — in @ConfigurationProperties or constants
@Value("${image.max-size-bytes:307200}")
private long maxImageSizeBytes;

@Value("${cache.product-ttl-seconds:60}")
private int productCacheTtlSeconds;
```

### RULE 7 — Avoid N+1 Query Problem

```java
// ❌ WRONG — 1 query for orders + 1 query per order for items = N+1
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> order.getItems().size()); // triggers N extra queries

// ✅ CORRECT — one query with JOIN
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.status = :status")
List<Order> findByStatusWithItems(@Param("status") OrderStatus status);
```

### RULE 8 — Separate Dev and Prod Config

```properties
# application-dev.properties
spring.datasource.url=jdbc:mysql://localhost:3306/lumoo_dev
spring.jpa.show-sql=true
spring.cache.type=simple
logging.level.com.lumoo=DEBUG

# application-prod.properties
spring.datasource.url=jdbc:mysql://mysql:3306/lumoo_db
spring.jpa.show-sql=false
spring.cache.type=redis
logging.level.com.lumoo=INFO
server.tomcat.threads.max=200
```

---

## Database Schema — Generate This Exactly

```sql
-- V1__init_schema.sql (Flyway migration)

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL COMMENT 'Price in smallest GMD unit',
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0 COMMENT 'Optimistic locking version',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_active (active)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50),
    amount INT NOT NULL COMMENT 'Total in smallest GMD unit',
    currency VARCHAR(10) DEFAULT 'GMD',
    status ENUM('PENDING','PAID','FAILED','CANCELLED') DEFAULT 'PENDING',
    payment_id VARCHAR(255) UNIQUE COMMENT 'ModemPay payment intent ID',
    payment_link TEXT COMMENT 'ModemPay checkout URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_payment_id (payment_id),
    INDEX idx_order_status (status),
    INDEX idx_order_email (customer_email)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price INT NOT NULL,
    subtotal INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL COMMENT 'ModemPay event ID for idempotency',
    event_type VARCHAR(100) NOT NULL,
    payment_id VARCHAR(255),
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webhook_event_id (event_id),
    INDEX idx_webhook_payment_id (payment_id)
);

-- V2__add_security_tables.sql

-- Encrypt sensitive fields in orders table
ALTER TABLE orders
    ADD COLUMN customer_email_encrypted TEXT COMMENT 'AES-GCM encrypted email',
    ADD COLUMN customer_phone_encrypted TEXT COMMENT 'AES-GCM encrypted phone';

-- Audit log — append-only, never delete
CREATE TABLE audit_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id      BIGINT,
    user_email   VARCHAR(255),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    VARCHAR(255),
    old_value    MEDIUMTEXT COMMENT 'JSON before state',
    new_value    MEDIUMTEXT COMMENT 'JSON after state',
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    status       ENUM('SUCCESS','FAILED') DEFAULT 'SUCCESS',
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id)
) COMMENT 'Append-only audit trail — do not DELETE from this table';

-- Admin users
CREATE TABLE admin_users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) UNIQUE NOT NULL,
    password     VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed — never plain text',
    role         ENUM('SUPER_ADMIN','ADMIN') DEFAULT 'ADMIN',
    active       BOOLEAN DEFAULT TRUE,
    last_login   TIMESTAMP NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Failed login attempts (brute force protection)
CREATE TABLE login_attempts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    ip_address   VARCHAR(45),
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    success      BOOLEAN DEFAULT FALSE,
    INDEX idx_login_email (email),
    INDEX idx_login_ip (ip_address),
    INDEX idx_login_time (attempted_at)
);
```

---

## Critical Implementation Requirements

### 1. STOCK RACE CONDITION — Must implement pessimistic locking

```java
// ProductRepository.java — MUST include this
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") Long id);
```

```java
// Product.java entity — MUST include @Version
@Version
private Long version; // optimistic locking fallback
```

### 2. MODEMPAY CALL — Must be non-blocking (no .block())

```java
// PaymentService.java — use reactive WebClient correctly
public Mono<Map> createPaymentIntent(Map<String, Object> body) {
    return modemPayClient.post()
        .uri("/payment-intents")
        .bodyValue(body)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response ->
            response.bodyToMono(String.class)
                .flatMap(error -> Mono.error(new PaymentException("ModemPay error: " + error)))
        )
        .bodyToMono(Map.class);
}
```

### 3. WEBHOOK IDEMPOTENCY — Must prevent duplicate processing

```java
// WebhookService.java — check for duplicate events
@Transactional
public void processWebhook(String eventId, String eventType, String paymentId, String payload) {
    // 1. Check if already processed
    if (webhookEventRepository.existsByEventId(eventId)) {
        log.warn("Duplicate webhook received, skipping: {}", eventId);
        return;
    }

    // 2. Save event record first
    WebhookEvent event = new WebhookEvent();
    event.setEventId(eventId);
    event.setEventType(eventType);
    event.setPaymentId(paymentId);
    event.setPayload(payload);
    webhookEventRepository.save(event);

    // 3. Process based on type
    switch (eventType) {
        case "charge.succeeded" -> markOrderPaid(paymentId);
        case "charge.failed"    -> markOrderFailed(paymentId);
        default -> log.info("Unhandled event type: {}", eventType);
    }

    // 4. Mark as processed
    event.setProcessed(true);
    event.setProcessedAt(LocalDateTime.now());
    webhookEventRepository.save(event);
}

private void markOrderPaid(String paymentId) {
    orderRepository.findByPaymentId(paymentId)
        .filter(o -> o.getStatus() == OrderStatus.PENDING) // guard — PENDING only
        .ifPresent(o -> {
            o.setStatus(OrderStatus.PAID);
            o.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(o);
            // Reduce stock for each item
            o.getItems().forEach(item -> {
                productRepository.findById(item.getProductId()).ifPresent(p -> {
                    p.setStock(p.getStock() - item.getQuantity());
                    productRepository.save(p);
                });
            });
            log.info("Order {} marked PAID", o.getId());
            // TODO: trigger email confirmation
        });
}
```

### 4. CHECKOUT — Must be @Transactional with stock validation

```java
// OrderService.java
@Transactional
public Order createPendingOrder(CheckoutRequest request) {
    List<OrderItem> items = new ArrayList<>();
    int total = 0;

    for (CheckoutRequest.CartItem cartItem : request.getItems()) {
        // Use PESSIMISTIC lock to prevent overselling
        Product product = productRepository.findByIdWithLock(cartItem.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));

        if (!product.getActive()) {
            throw new RuntimeException("Product is no longer available: " + product.getName());
        }

        if (product.getStock() < cartItem.getQuantity()) {
            throw new InsufficientStockException(
                "Insufficient stock for: " + product.getName() +
                ". Available: " + product.getStock() +
                ", Requested: " + cartItem.getQuantity()
            );
        }

        int subtotal = product.getPrice() * cartItem.getQuantity();
        total += subtotal;

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(cartItem.getQuantity());
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(subtotal);
        items.add(item);
    }

    Order order = new Order();
    order.setCustomerName(request.getCustomerName());
    order.setCustomerEmail(request.getCustomerEmail());
    order.setCustomerPhone(request.getCustomerPhone());
    order.setAmount(total);
    order.setCurrency("GMD");
    order.setStatus(OrderStatus.PENDING);
    order.setCreatedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    Order savedOrder = orderRepository.save(order);

    items.forEach(i -> i.setOrder(savedOrder));
    savedOrder.setItems(items);
    return orderRepository.save(savedOrder);
}
```

### 5. WEBHOOK SIGNATURE VALIDATION — Must be included

```java
// WebhookController.java
private boolean isValidSignature(String payload, String signature) {
    try {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        ));
        String computed = HexFormat.of().formatHex(
            mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
        );
        return computed.equals(signature);
    } catch (Exception e) {
        log.error("Signature validation error", e);
        return false;
    }
}
```

### 6. CACHING — Must cache product listings

```java
// ProductService.java
@Cacheable(value = "products", key = "'all-active'")
public List<Product> getAllActiveProducts() {
    return productRepository.findByActiveTrue();
}

@CacheEvict(value = "products", allEntries = true)
public Product saveProduct(Product product) {
    return productRepository.save(product);
}

@CacheEvict(value = "products", allEntries = true)
public void deleteProduct(Long id) {
    productRepository.deleteById(id);
}
```

---

## application.properties — Full Config

```properties
# App
spring.application.name=lumoo
app.base-url=https://lumoo.my

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/lumoo_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# HikariCP Connection Pool — CRITICAL for concurrency
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=LumooHikariPool

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true

# Flyway DB Migration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Redis Cache
spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.cache.redis.time-to-live=300000

# ModemPay
modempay.api-key=${MODEMPAY_API_KEY}
modempay.webhook-secret=${MODEMPAY_WEBHOOK_SECRET}
modempay.base-url=https://api.modempay.com

# Logging
logging.level.com.lumoo=INFO
logging.level.org.springframework.web=WARN

# Actuator (health checks)
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# Server
server.port=8080
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20
server.tomcat.accept-count=100
```

---

## docker-compose.yml — Full Setup

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_USERNAME=lumoo
      - DB_PASSWORD=lumoo_secret
      - MODEMPAY_API_KEY=${MODEMPAY_API_KEY}
      - MODEMPAY_WEBHOOK_SECRET=${MODEMPAY_WEBHOOK_SECRET}
      - REDIS_HOST=redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_secret
      MYSQL_DATABASE: lumoo_db
      MYSQL_USER: lumoo
      MYSQL_PASSWORD: lumoo_secret
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
```

---

## pom.xml Dependencies to Include

```xml
<dependencies>
    <!-- Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>

    <!-- Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Rate Limiting -->
    <dependency>
        <groupId>com.github.vladimir-bukhtoyarov</groupId>
        <artifactId>bucket4j-core</artifactId>
        <version>7.6.0</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Scalability Targets

This architecture should handle:
- ✅ 1–20 concurrent users — effortlessly
- ✅ 20–150 concurrent users — with connection pool + caching
- ✅ 150–500 concurrent users — with Redis + proper indexing + Nginx
- ⚠️ 500+ concurrent users — would need horizontal scaling (multiple instances + load balancer)

For a Gambian e-commerce store at launch, 150 concurrent users is more than sufficient.

---

## API Endpoints to Implement

### Public Endpoints (no auth required)
```
GET    /api/products              - List all active products (cached)
GET    /api/products/{id}         - Get single product
POST   /api/checkout              - Create order + get payment link
GET    /api/payment/success       - Payment success redirect handler
GET    /api/payment/cancel        - Payment cancel redirect handler
POST   /api/webhooks/modempay     - ModemPay webhook receiver
```

### Admin Endpoints (JWT auth required)
```
POST   /api/admin/products        - Create product
PUT    /api/admin/products/{id}   - Update product
DELETE /api/admin/products/{id}   - Delete product
GET    /api/admin/orders          - List all orders (with pagination)
GET    /api/admin/orders/{id}     - Get order detail
GET    /api/admin/orders/status/{status} - Filter by status
PUT    /api/admin/orders/{id}/cancel     - Cancel order
```

---

## ModemPay Payment Flow

```
1. Customer POSTs to /api/checkout with cart items
2. App validates stock (with DB lock)
3. App saves Order with status=PENDING
4. App calls ModemPay POST /payment-intents
5. ModemPay returns { data: { id, payment_link } }
6. App saves payment_id + payment_link to Order
7. App returns { orderId, paymentLink } to frontend
8. Frontend redirects customer to paymentLink
9. Customer pays via QMoney/Afrimoney/Wave on ModemPay checkout
10. ModemPay sends webhook POST to /api/webhooks/modempay
11. App validates HMAC signature
12. App checks idempotency (WebhookEvent table)
13. App marks Order PAID, reduces stock
14. Customer lands on return_url (/api/payment/success?orderId=X)
```

---

## ModemPay API Details

- **Base URL:** `https://api.modempay.com`
- **Auth:** `Authorization: Bearer YOUR_API_KEY`
- **Create payment intent:** `POST /payment-intents`
- **Currency:** `GMD` (Gambian Dalasi)
- **Webhook signature header:** `x-modem-signature` (HMAC-SHA256)
- **Webhook events:**
  - `charge.succeeded` → mark order PAID
  - `charge.failed` → mark order FAILED
  - `payment_intent.created` → log only
  - `payment_intent.cancelled` → mark order CANCELLED

### Payment intent request body:
```json
{
  "amount": 4500,
  "currency": "GMD",
  "customer_name": "John Smith",
  "customer_email": "john@example.com",
  "customer_phone": "7000001",
  "return_url": "https://lumoo.my/payment/success?orderId=123",
  "cancel_url": "https://lumoo.my/payment/cancel?orderId=123",
  "metadata": { "order_id": 123 }
}
```

---

## Security Requirements — Enterprise Grade (Setaraf Pos DigiCert Standard)

> ⚠️ Ini bukan optional. Sistem kewangan yang bocor boleh menyebabkan saman beratus ribu.
> Implement SEMUA layer di bawah sebelum go live.

---

### LAYER 1 — Transport Security (nginx/nginx.conf)

```nginx
server {
    listen 443 ssl http2;
    server_name lumoo.my;

    ssl_certificate     /etc/letsencrypt/live/lumoo.my/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/lumoo.my/privkey.pem;

    # TLS 1.2 minimum — TLS 1.0/1.1 deprecated
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers — WAJIB ada
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    # Hide server version
    server_tokens off;

    location / {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Redirect semua HTTP ke HTTPS
server {
    listen 80;
    server_name lumoo.my;
    return 301 https://$host$request_uri;
}
```

---

### LAYER 2 — Data Encryption at Rest (security/DataEncryptor.java)

```java
@Component
public class DataEncryptor {

    @Value("${encryption.key}")
    private String encryptionKey;

    // AES-256-GCM encryption
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey), "AES"
            );
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted — both needed for decryption
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] combined  = Base64.getDecoder().decode(encryptedText);
            byte[] iv        = Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);

            SecretKeySpec key = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey), "AES"
            );
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

Apply encryption on Order entity:

```java
// entity/Order.java — encrypt customer PII
@Entity
@Table(name = "orders")
public class Order {

    // Store encrypted in DB
    @Column(name = "customer_email_encrypted")
    private String customerEmailEncrypted;

    @Column(name = "customer_phone_encrypted")
    private String customerPhoneEncrypted;

    // Transient — not stored, only used in memory
    @Transient private String customerEmail;
    @Transient private String customerPhone;
    @Transient private DataEncryptor encryptor;

    @PostLoad
    public void decryptFields() {
        this.customerEmail = encryptor.decrypt(this.customerEmailEncrypted);
        this.customerPhone = encryptor.decrypt(this.customerPhoneEncrypted);
    }

    @PrePersist
    @PreUpdate
    public void encryptFields() {
        this.customerEmailEncrypted = encryptor.encrypt(this.customerEmail);
        this.customerPhoneEncrypted = encryptor.encrypt(this.customerPhone);
    }
}
```

---

### LAYER 3 — Audit Trail (service/AuditService.java)

**Ini yang akan selamatkan nyawa kat mahkamah. Semua transaksi kewangan MESTI dilog.**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(String action,
                    String entityType,
                    String entityId,
                    Object oldValue,
                    Object newValue,
                    HttpServletRequest request) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setOldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null);
            entry.setNewValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null);
            entry.setIpAddress(getClientIp(request));
            entry.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
            entry.setTimestamp(LocalDateTime.now());
            entry.setStatus("SUCCESS");
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit failure break the main flow — just log it
            log.error("Failed to write audit log for action: {}", action, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "SYSTEM";
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
```

Call audit log in OrderService:

```java
// Every payment state change MUST be audited
auditService.log("ORDER_PAID", "Order", order.getId().toString(),
                 previousState, currentState, request);

auditService.log("ORDER_CREATED", "Order", order.getId().toString(),
                 null, order, request);

auditService.log("STOCK_REDUCED", "Product", product.getId().toString(),
                 Map.of("stock", oldStock), Map.of("stock", newStock), request);
```

---

### LAYER 4 — Input Validation (dto/CheckoutRequest.java)

```java
@Data
public class CheckoutRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Pattern(regexp = "^[\\p{L}\\s'\\-\\.]+$", message = "Invalid characters in name")
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email too long")
    private String customerEmail;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    private String customerPhone;

    @NotEmpty(message = "Cart cannot be empty")
    @Size(min = 1, max = 50, message = "Cart must have 1 to 50 items")
    @Valid
    private List<CartItem> items;

    @Data
    public static class CartItem {
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Minimum quantity is 1")
        @Max(value = 100, message = "Maximum quantity per item is 100")
        private Integer quantity;
    }
}
```

---

### LAYER 5 — Rate Limiting (config/RateLimitConfig.java)

```java
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Checkout: 5 requests per minute per IP (brute force protection)
    public Bucket checkoutBucketFor(String ip) {
        return buckets.computeIfAbsent("checkout:" + ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build()
        );
    }

    // General API: 100 requests per minute per IP
    public Bucket apiBucketFor(String ip) {
        return buckets.computeIfAbsent("api:" + ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build()
        );
    }
}
```

Apply in CheckoutController:

```java
@PostMapping("/checkout")
public ResponseEntity<?> checkout(
    @RequestBody @Valid CheckoutRequest request,
    HttpServletRequest httpRequest
) {
    String ip = getClientIp(httpRequest);

    if (!rateLimitConfig.checkoutBucketFor(ip).tryConsume(1)) {
        auditService.log("RATE_LIMIT_EXCEEDED", "Checkout", null, null,
                        Map.of("ip", ip), httpRequest);
        return ResponseEntity.status(429).body(Map.of(
            "error", "TOO_MANY_REQUESTS",
            "message", "Too many checkout attempts. Please wait 1 minute."
        ));
    }

    return ResponseEntity.ok(orderService.checkout(request));
}
```

---

### LAYER 6 — JWT Authentication (security/JwtTokenProvider.java)

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final long ACCESS_TOKEN_EXPIRY  = 15 * 60 * 1000L;  // 15 minutes
    private final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000L; // 7 days

    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .claim("type", "ACCESS")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
            .setSubject(email)
            .claim("type", "REFRESH")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
```

---

### LAYER 7 — Brute Force Login Protection

```java
// service/LoginAttemptService.java
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    public boolean isBlocked(String email, String ip) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        long failedAttempts = loginAttemptRepository
            .countByEmailAndSuccessAndAttemptedAtAfter(email, false, since);
        return failedAttempts >= MAX_ATTEMPTS;
    }

    public void recordAttempt(String email, String ip, boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setIpAddress(ip);
        attempt.setSuccess(success);
        attempt.setAttemptedAt(LocalDateTime.now());
        loginAttemptRepository.save(attempt);
    }
}
```

---

### LAYER 8 — Global Exception Handler (never expose internals)

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(409).body(new ErrorResponse(409, "INSUFFICIENT_STOCK", e.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException e) {
        log.error("Payment error: {}", e.getMessage()); // log full error server-side
        return ResponseEntity.status(502).body(
            new ErrorResponse(502, "PAYMENT_ERROR", "Payment processing failed. Please try again.")
            // ← Never return e.getMessage() to client — may expose internal details
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(new ErrorResponse(400, "VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Unhandled exception", e); // full stack trace server-side only
        return ResponseEntity.status(500).body(
            new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred.")
            // ← Never expose stack trace or internal message to client
        );
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String timestamp = LocalDateTime.now().toString();

        public ErrorResponse(int status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }
    }
}
```

---

### LAYER 9 — .gitignore (WAJIB — kalau lupa ini, API key bocor kat GitHub)

```gitignore
# Environment secrets — NEVER commit these
.env
*.env.local
*.env.production
application-prod.properties
application-local.properties

# Build artifacts
target/
*.jar
*.war
*.class

# IDE files
.idea/
*.iml
.vscode/
*.suo

# OS files
.DS_Store
Thumbs.db

# Logs — may contain sensitive data
logs/
*.log

# SSL certificates
*.pem
*.key
*.crt
```

---

### LAYER 10 — Secret Management (.env.example)

```env
# ModemPay — get from merchant.modempay.com
MODEMPAY_API_KEY=pk_test_your_key_here
MODEMPAY_WEBHOOK_SECRET=wh_your_secret_here

# Database
DB_USERNAME=lumoo
DB_PASSWORD=use_a_strong_password_minimum_20_chars

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Encryption — generate with: openssl rand -base64 32
ENCRYPTION_KEY=your_base64_256bit_key_here

# JWT — generate with: openssl rand -base64 64
JWT_SECRET=your_minimum_256bit_jwt_secret_here

# App
APP_BASE_URL=https://lumoo.my

# SSL (production)
SSL_CERT_PATH=/etc/letsencrypt/live/lumoo.my/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/lumoo.my/privkey.pem
```

---

### Admin Audit Endpoints (controller/AdminAuditController.java)

```
GET /api/admin/audit?action=ORDER_PAID&from=2026-01-01&to=2026-12-31
GET /api/admin/audit?entityType=Order&entityId=123
GET /api/admin/audit?ipAddress=192.168.1.1
```

These endpoints allow your client to prove to any court or auditor exactly what happened, when, and from which IP address.

---

## Error Handling — GlobalExceptionHandler

Handle these exceptions with proper HTTP status codes:
- `InsufficientStockException` → 409 Conflict
- `EntityNotFoundException` → 404 Not Found
- `PaymentException` → 502 Bad Gateway
- `MethodArgumentNotValidException` → 400 Bad Request
- `OptimisticLockException` → 409 Conflict (retry message)
- `Exception` (generic) → 500 Internal Server Error

All errors return:
```json
{
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for: Blue T-Shirt. Available: 2, Requested: 5",
  "timestamp": "2026-05-27T10:30:00"
}
```

---

## Unit Tests to Write

```
OrderServiceTest
  ✓ checkout_success_createsOrder
  ✓ checkout_insufficientStock_throwsException
  ✓ checkout_productNotFound_throwsException
  ✓ checkout_inactiveProduct_throwsException
  ✓ markOrderPaid_pendingOrder_marksAsPaid
  ✓ markOrderPaid_alreadyPaid_doesNothing          ← idempotency
  ✓ markOrderPaid_reducesStockCorrectly

WebhookServiceTest
  ✓ processWebhook_chargeSucceeded_marksOrderPaid
  ✓ processWebhook_duplicateEventId_skipsProcessing ← idempotency
  ✓ processWebhook_chargeFailed_marksOrderFailed

ProductServiceTest
  ✓ getAllProducts_returnsCachedResults
  ✓ saveProduct_evictsCacheAndSaves

WebhookControllerTest
  ✓ handleWebhook_validSignature_returns200
  ✓ handleWebhook_invalidSignature_returns400
  ✓ handleWebhook_missingSignatureHeader_returns400

SecurityTest
  ✓ adminEndpoint_withoutJwt_returns401
  ✓ adminEndpoint_withValidJwt_returns200
  ✓ adminEndpoint_withExpiredJwt_returns401
  ✓ checkout_exceedsRateLimit_returns429
  ✓ checkout_invalidInput_returns400
  ✓ login_exceeds5FailedAttempts_blocksAccount

EncryptionTest
  ✓ encrypt_thenDecrypt_returnsOriginal
  ✓ encrypt_nullInput_returnsNull
  ✓ orderSaved_emailIsEncryptedInDb
  ✓ orderLoaded_emailIsDecrypted

AuditTest
  ✓ orderPaid_auditLogCreated
  ✓ auditLog_containsIpAddress
  ✓ auditLog_containsBeforeAndAfterState
```

---

## README.md to Generate

Include:
1. Project overview (Lumoo.my — Gambian e-commerce platform)
2. Tech stack list
3. Prerequisites (Java 17, Docker, Docker Compose)
4. Setup instructions (clone → configure .env → docker-compose up)
5. Environment variables table (MODEMPAY_API_KEY, MODEMPAY_WEBHOOK_SECRET, DB_USERNAME, DB_PASSWORD)
6. API endpoints table
7. ModemPay integration notes (test mode vs live mode)
8. Architecture diagram (text-based)
9. How to run tests (`./mvnw test`)

---

## .env Template to Generate

```env
# ModemPay (get from merchant.modempay.com dashboard)
MODEMPAY_API_KEY=pk_test_your_key_here
MODEMPAY_WEBHOOK_SECRET=wh_your_secret_here

# Database
DB_USERNAME=lumoo
DB_PASSWORD=use_strong_password_minimum_20_chars

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Encryption — generate with: openssl rand -base64 32
ENCRYPTION_KEY=your_base64_256bit_key_here

# JWT — generate with: openssl rand -base64 64
JWT_SECRET=your_minimum_256bit_jwt_secret_here

# App
APP_BASE_URL=https://lumoo.my
```

---

## What NOT to Do

**Architecture**
- ❌ Do NOT use `.block()` on WebClient calls — keep reactive
- ❌ Do NOT reduce stock on order creation — only reduce on webhook `charge.succeeded`
- ❌ Do NOT skip signature validation on webhooks
- ❌ Do NOT use `spring.jpa.hibernate.ddl-auto=create` or `update` in production — use Flyway
- ❌ Do NOT process duplicate webhooks — always check WebhookEvent table first

**Security**
- ❌ Do NOT store API keys, secrets, or passwords in application.properties or source code
- ❌ Do NOT commit .env file to GitHub — add to .gitignore
- ❌ Do NOT log customer email, phone, payment data, or API keys
- ❌ Do NOT expose stack traces or internal error messages to API clients
- ❌ Do NOT store passwords in plain text — always BCrypt with cost factor 12+
- ❌ Do NOT store customer email/phone unencrypted in database
- ❌ Do NOT allow unlimited login attempts — implement brute force protection
- ❌ Do NOT skip rate limiting on checkout — bots will abuse it
- ❌ Do NOT use HTTP in production — HTTPS only, redirect all HTTP
- ❌ Do NOT use TLS 1.0 or 1.1 — TLS 1.2 minimum
- ❌ Do NOT use string concatenation in SQL queries — use JPA parameterised queries only
- ❌ Do NOT delete from audit_logs table — it is append-only evidence
- ❌ Do NOT return 500 errors without logging the full exception server-side first

---

## After Implementation Checklist

**Architecture**
- [ ] All unit tests pass (`./mvnw test`)
- [ ] Docker Compose starts cleanly (`docker-compose up`)
- [ ] `/actuator/health` returns UP
- [ ] Product listing is cached (second request faster than first)
- [ ] Checkout creates PENDING order in DB
- [ ] Webhook with valid signature updates order to PAID and reduces stock
- [ ] Webhook with invalid signature returns 400
- [ ] Duplicate webhook is ignored (idempotency works)
- [ ] Concurrent checkout requests don't oversell stock

**Security**
- [ ] All HTTP redirects to HTTPS (test with curl http://lumoo.my)
- [ ] Security headers present (test with securityheaders.com)
- [ ] TLS rating A or A+ (test with ssllabs.com/ssltest)
- [ ] Customer email is encrypted in DB (check raw DB row)
- [ ] .env not committed to GitHub (check .gitignore)
- [ ] API keys only in environment variables
- [ ] Rate limit works (hammer checkout 6x → 429 on 6th)
- [ ] Admin endpoint returns 401 without JWT
- [ ] 5 failed logins → account locked for 15 minutes
- [ ] Every ORDER_PAID has an audit log entry
- [ ] Audit log contains IP address and timestamp
- [ ] Error responses don't expose internal details
- [ ] README is complete and accurate
- [ ] No secrets in codebase (run: `git grep -i "password\|secret\|api_key"` — should be empty)

---

## Gambia User Profile — Build For This Reality

> ⚠️ This is NOT a Malaysian audience. Every API and performance decision must account for these constraints.

| Factor | Reality |
|---|---|
| **Device** | Android 90%+ — low-end phones (1–2GB RAM) |
| **Browser** | Chrome Mobile dominant — no Safari, no desktop |
| **Internet speed** | 2–5 Mbps average (among slowest globally) |
| **Latency** | 100–110ms ping to external servers |
| **Data cost** | Very expensive — up to $100/month for high-speed |
| **Connection type** | Mobile data (Africell, QCell) — rarely home WiFi |
| **Internet users** | ~1.3 million out of 2.8 million population |
| **Language** | English (official) |
| **Currency** | GMD (Gambian Dalasi) |
| **Payment method** | Mobile money ONLY — QMoney, Afrimoney, Wave |

**Core principle: Every byte costs a Gambian user real money. Treat bandwidth as scarce — because it is.**

---

## Performance Requirements — Gambia-Optimised

### REQUIREMENT 1 — API Response Must Be Minimal

Never return unused fields. Gambia users on 2 Mbps feel every extra byte.

```java
// dto/ProductSummaryDTO.java — for product LISTING (minimal)
// Only return what the product card needs to display
@Data
@AllArgsConstructor
public class ProductSummaryDTO {
    private Long id;
    private String name;
    private Integer price;       // in GMD smallest unit
    private String imageUrl;     // thumbnail URL only
    private Boolean inStock;     // true/false — not the actual count
    // ← NO description, NO createdAt, NO updatedAt, NO version
}

// dto/ProductDetailDTO.java — for single product page (full)
@Data
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private String imageUrl;
    private Boolean active;
    // ← Full details only when user taps on a specific product
}
```

Repository projection for listing:

```java
// repository/ProductRepository.java
@Query("SELECT new com.lumoo.dto.ProductSummaryDTO(" +
       "p.id, p.name, p.price, p.imageUrl, (p.stock > 0)) " +
       "FROM Product p WHERE p.active = true ORDER BY p.id DESC")
Page<ProductSummaryDTO> findAllSummary(Pageable pageable);
```

---

### REQUIREMENT 2 — Pagination Mandatory (never return all products)

```java
// controller/ProductController.java
@GetMapping("/products")
public ResponseEntity<Page<ProductSummaryDTO>> getProducts(
    @RequestParam(defaultValue = "0")  int page,
    @RequestParam(defaultValue = "12") int size   // 12 per page — fits mobile screen
) {
    // Cap at 20 max per page — never let client request 1000 products
    int safeSizeLimit = Math.min(size, 20);
    Pageable pageable = PageRequest.of(page, safeSizeLimit, Sort.by("id").descending());
    return ResponseEntity.ok(productService.getActiveProducts(pageable));
}
```

Response includes pagination metadata so frontend knows when to stop:

```json
{
  "content": [...],
  "page": { "number": 0, "size": 12, "totalElements": 48, "totalPages": 4 }
}
```

---

### REQUIREMENT 3 — HTTP Compression (reduces payload 60–80%)

```properties
# application.properties — compress all JSON responses
server.compression.enabled=true
server.compression.mime-types=application/json,text/html,text/plain,text/xml
server.compression.min-response-size=512
```

Effect on product listing:
- Uncompressed JSON: ~8 KB for 12 products
- Gzip compressed: ~1.5 KB
- **Saves ~6.5 KB per listing request — matters on 2 Mbps**

---

### REQUIREMENT 4 — Image Upload Validation & Compression

```java
// service/ImageService.java
@Service
public class ImageService {

    private static final long MAX_IMAGE_SIZE = 300_000L; // 300KB max upload
    private static final int  TARGET_WIDTH   = 600;       // px — enough for mobile
    private static final int  JPEG_QUALITY   = 75;        // % — good quality, small size

    public String processAndSave(MultipartFile file) throws IOException {

        // 1. Reject oversized uploads
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                "Image too large: " + (file.getSize() / 1024) + "KB. Maximum is 300KB."
            );
        }

        // 2. Validate it's actually an image
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (JPEG, PNG, WebP)");
        }

        // 3. Read and resize
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) throw new IllegalArgumentException("Cannot read image file");

        // Resize to max 600px wide, maintain aspect ratio
        BufferedImage resized = resizeImage(original, TARGET_WIDTH);

        // 4. Save as JPEG with compression
        String filename = UUID.randomUUID() + ".jpg";
        File output = new File("/app/images/" + filename);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY / 100f);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resized, null, null), param);
        }

        // Return relative URL
        return "/images/" + filename;
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth) {
        if (original.getWidth() <= targetWidth) return original; // already small enough

        int targetHeight = (int) ((double) original.getHeight() / original.getWidth() * targetWidth);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(
            original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH),
            0, 0, null
        );
        return resized;
    }
}
```

---

### REQUIREMENT 5 — Caching Strategy (reduce DB hits)

```java
// service/ProductService.java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // Cache product listing — invalidate when any product changes
    @Cacheable(value = "products-page", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductSummaryDTO> getActiveProducts(Pageable pageable) {
        return productRepository.findAllSummary(pageable);
    }

    // Cache single product detail
    @Cacheable(value = "product-detail", key = "#id")
    public ProductDetailDTO getProductById(Long id) {
        return productRepository.findById(id)
            .map(this::toDetailDTO)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    // Evict ALL product caches when anything changes
    @Caching(evict = {
        @CacheEvict(value = "products-page",   allEntries = true),
        @CacheEvict(value = "product-detail",  allEntries = true)
    })
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Caching(evict = {
        @CacheEvict(value = "products-page",   allEntries = true),
        @CacheEvict(value = "product-detail",  key = "#id")
    })
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

Cache TTL configuration:

```properties
# application.properties
# Products change rarely — cache for 5 minutes
spring.cache.redis.time-to-live=300000

# Configure per-cache TTL in CacheConfig.java:
# products-page  → 5 minutes
# product-detail → 10 minutes
# orders         → NO cache (always fresh)
```

---

### REQUIREMENT 6 — Response Headers for Client Caching

```java
// controller/ProductController.java
@GetMapping("/products/{id}")
public ResponseEntity<ProductDetailDTO> getProduct(@PathVariable Long id) {
    ProductDetailDTO product = productService.getProductById(id);

    return ResponseEntity.ok()
        // Tell browser to cache for 60 seconds — reduces repeat requests
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
        .eTag(String.valueOf(product.hashCode())) // ETag for conditional requests
        .body(product);
}

@GetMapping("/products")
public ResponseEntity<Page<ProductSummaryDTO>> getProducts(...) {
    Page<ProductSummaryDTO> products = productService.getActiveProducts(pageable);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
        .body(products);
}
```

---

### REQUIREMENT 7 — Nginx Static File Serving + Caching

```nginx
# nginx/nginx.conf — add to existing config

# Serve product images directly from Nginx (NOT Spring Boot)
# Spring Boot should never serve static files
location /images/ {
    root /app;
    expires 30d;                                    # Browser cache 30 days
    add_header Cache-Control "public, immutable";   # Immutable = never re-fetch
    add_header Vary "Accept-Encoding";

    # Enable gzip for images (WebP/JPEG already compressed — skip)
    gzip off;

    # Limit image size served (prevent abuse)
    client_max_body_size 5m;
}

# Gzip everything else
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_types
    application/json
    application/javascript
    text/css
    text/html
    text/plain;
gzip_comp_level 6;

# Connection keep-alive — reduce TCP handshake overhead
# (Matters more on high-latency Gambia connections)
keepalive_timeout 65;
keepalive_requests 100;
```

---

### REQUIREMENT 8 — Cloudflare Configuration (add to docker-compose + README)

Cloudflare free tier is **mandatory** for lumoo.my to be usable in Gambia.

```
Without Cloudflare:
  Gambian user → Malaysia server → 250ms+ latency

With Cloudflare:
  Gambian user → Cloudflare Africa edge → cached response → ~50ms
```

Setup instructions to include in README:

```
1. Sign up at cloudflare.com (free)
2. Add lumoo.my domain
3. Change nameservers to Cloudflare's
4. Enable settings:
   - SSL/TLS: Full (Strict)
   - Always Use HTTPS: ON
   - Auto Minify: JavaScript ✓, CSS ✓, HTML ✓
   - Brotli compression: ON
   - Browser Cache TTL: 4 hours
   - Rocket Loader: ON (async JS loading)
5. Create Page Rules:
   - lumoo.my/images/*  → Cache Level: Cache Everything, Edge TTL: 1 month
   - lumoo.my/api/*     → Cache Level: Bypass (never cache API)
```

---

### REQUIREMENT 9 — pom.xml Additional Dependencies for Performance

```xml
<!-- Image processing -->
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-jpeg</artifactId>
    <version>3.10.1</version>
</dependency>
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-webp</artifactId>
    <version>3.10.1</version>
</dependency>
```

---

### REQUIREMENT 10 — Performance Targets to Validate

After implementation, test from a simulated slow connection (use Chrome DevTools → Network → Slow 3G):

| Metric | Target | Why |
|---|---|---|
| Product listing API response | < 200ms | Cached Redis response |
| Product listing payload | < 5 KB for 12 items | Compressed JSON, minimal fields |
| Single product image | < 80 KB | 600px JPEG at 75% quality |
| Checkout API response | < 500ms | DB write + ModemPay call |
| Total page load (first visit) | < 3 seconds on 2 Mbps | Gambia average speed |
| Total page load (repeat visit) | < 1 second | Cloudflare + browser cache |

---

## Performance — What NOT to Do

- ❌ Do NOT return full Product entity from listing endpoint — use DTO with minimal fields
- ❌ Do NOT load all products without pagination — use page size 12 max
- ❌ Do NOT serve images through Spring Boot — use Nginx static serving
- ❌ Do NOT store images > 300KB — compress and resize on upload
- ❌ Do NOT skip HTTP compression — gzip saves 60–80% bandwidth
- ❌ Do NOT skip Cloudflare setup — 250ms+ round-trip is unusable on slow connections
- ❌ Do NOT include timestamps/version/internal fields in public API responses
- ❌ Do NOT cache order or payment data — always fetch fresh from DB

---

## Updated After Implementation Checklist

**Architecture**
- [ ] All unit tests pass (`./mvnw test`)
- [ ] Docker Compose starts cleanly (`docker-compose up`)
- [ ] `/actuator/health` returns UP
- [ ] Product listing is cached (second request faster than first)
- [ ] Checkout creates PENDING order in DB
- [ ] Webhook with valid signature updates order to PAID and reduces stock
- [ ] Webhook with invalid signature returns 400
- [ ] Duplicate webhook is ignored (idempotency works)
- [ ] Concurrent checkout requests don't oversell stock

**Security**
- [ ] All HTTP redirects to HTTPS (test with `curl http://lumoo.my`)
- [ ] Security headers present (test with securityheaders.com)
- [ ] TLS rating A or A+ (test with ssllabs.com/ssltest)
- [ ] Customer email is encrypted in DB (check raw DB row)
- [ ] .env not committed to GitHub (check .gitignore)
- [ ] API keys only in environment variables
- [ ] Rate limit works (hammer checkout 6x → 429 on 6th request)
- [ ] Admin endpoint returns 401 without JWT
- [ ] 5 failed logins → account locked for 15 minutes
- [ ] Every ORDER_PAID has an audit log entry
- [ ] Audit log contains IP address and before/after state
- [ ] Error responses don't expose internal details
- [ ] No secrets in codebase (`git grep -i "password\|secret\|api_key"` → empty)

**Performance — Gambia**
- [ ] Product listing payload < 5 KB for 12 items (check Network tab)
- [ ] HTTP compression enabled (response has `Content-Encoding: gzip` header)
- [ ] Images served by Nginx, NOT Spring Boot
- [ ] Uploaded images compressed and resized to max 600px / 80KB
- [ ] Pagination working — `/api/products?page=0&size=12` returns 12 items
- [ ] Product listing returns only: id, name, price, imageUrl, inStock
- [ ] Browser cache headers on product endpoints (check `Cache-Control` header)
- [ ] Cloudflare configured and proxying lumoo.my (orange cloud in Cloudflare dashboard)
- [ ] Second visit to product listing < 1 second (Cloudflare edge cache)
- [ ] Simulated 2 Mbps load test: product listing loads in < 3 seconds

---

## Gambia PDPP 2025 Compliance — Personal Data Protection & Privacy Act

> ⚠️ KRITIKAL: Gambia baru sahaja meluluskan Personal Data Protection and Privacy Act 2025
> (dikuatkuasakan 7 November 2025). Lumoo.my sebagai platform e-commerce yang handle
> customer data WAJIB comply. Penalti boleh cecah 4% daripada annual turnover.
> Ini setaraf GDPR Europe dari segi kepentingan.

---

### REQUIREMENT 1 — Privacy Policy Page

Generate endpoint yang return Privacy Policy:

```java
// controller/LegalController.java
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    @GetMapping("/privacy-policy")
    public ResponseEntity<Map<String, Object>> privacyPolicy() {
        return ResponseEntity.ok(Map.of(
            "lastUpdated", "2026-01-01",
            "companyName", "Lumoo",
            "jurisdiction", "The Gambia",
            "governingLaw", "Personal Data Protection and Privacy Act 2025 (PDPP)",
            "dataController", Map.of(
                "name", "Lumoo E-Commerce",
                "email", "privacy@lumoo.my",
                "country", "The Gambia"
            ),
            "dataCollected", List.of(
                "Full name — for order processing",
                "Email address — for order confirmation",
                "Phone number — for delivery coordination",
                "Purchase history — for order management"
            ),
            "dataRetention", "Order data retained for 7 years (legal requirement)",
            "userRights", List.of(
                "Right to access your personal data",
                "Right to correct inaccurate data",
                "Right to erasure (right to be forgotten)",
                "Right to data portability",
                "Right to object to processing"
            ),
            "contact", "privacy@lumoo.my"
        ));
    }

    @GetMapping("/terms")
    public ResponseEntity<Map<String, Object>> terms() {
        return ResponseEntity.ok(Map.of(
            "lastUpdated", "2026-01-01",
            "jurisdiction", "The Gambia",
            "governingLaw", "Information and Communications Act 2009",
            "currency", "GMD (Gambian Dalasi)",
            "paymentMethods", List.of("QMoney", "Afrimoney", "Wave")
        ));
    }
}
```

---

### REQUIREMENT 2 — Consent at Checkout (PDPP s.12 — Lawful Basis)

PDPP 2025 requires explicit consent before collecting personal data.

```java
// dto/CheckoutRequest.java — add consent fields
@Data
public class CheckoutRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255)
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email
    private String customerEmail;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$")
    private String customerPhone;

    @NotEmpty
    private List<CartItem> items;

    // PDPP 2025 — explicit consent required
    @AssertTrue(message = "You must accept the Privacy Policy to proceed")
    private Boolean privacyPolicyAccepted;

    @AssertTrue(message = "You must accept the Terms and Conditions to proceed")
    private Boolean termsAccepted;

    // Optional — marketing consent (separate from required consents)
    private Boolean marketingEmailConsent = false;
}
```

```java
// entity/Order.java — store consent record
@Entity
public class Order {
    // ... existing fields ...

    // PDPP 2025 — consent audit trail
    @Column(name = "privacy_accepted", nullable = false)
    private Boolean privacyPolicyAccepted;

    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted;

    @Column(name = "marketing_consent")
    private Boolean marketingEmailConsent = false;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;

    @Column(name = "consent_ip")
    private String consentIpAddress;
}
```

```java
// service/OrderService.java — record consent with order
@Transactional
public Order createPendingOrder(CheckoutRequest request, HttpServletRequest httpRequest) {
    // ... existing stock validation ...

    order.setPrivacyPolicyAccepted(request.getPrivacyPolicyAccepted());
    order.setTermsAccepted(request.getTermsAccepted());
    order.setMarketingEmailConsent(request.getMarketingEmailConsent());
    order.setConsentTimestamp(LocalDateTime.now());
    order.setConsentIpAddress(getClientIp(httpRequest));

    // Audit the consent
    auditService.log("CONSENT_RECORDED", "Order", order.getId().toString(),
        null,
        Map.of(
            "privacyAccepted", true,
            "termsAccepted", true,
            "marketing", request.getMarketingEmailConsent(),
            "ip", getClientIp(httpRequest),
            "timestamp", LocalDateTime.now()
        ),
        httpRequest
    );
}
```

---

### REQUIREMENT 3 — Right to Erasure (PDPP s.24 — Right to be Forgotten)

```java
// controller/CustomerRightsController.java
@RestController
@RequestMapping("/api/customer-rights")
@RequiredArgsConstructor
@Slf4j
public class CustomerRightsController {

    private final CustomerRightsService customerRightsService;
    private final AuditService auditService;

    // Customer requests deletion of their data
    @PostMapping("/erasure-request")
    public ResponseEntity<?> requestErasure(
        @RequestParam @Email String email,
        HttpServletRequest request
    ) {
        customerRightsService.processErasureRequest(email, request);
        return ResponseEntity.ok(Map.of(
            "message", "Your erasure request has been received. We will process it within 30 days as required by the PDPP 2025.",
            "referenceId", UUID.randomUUID().toString()
        ));
    }

    // Customer requests copy of their data
    @PostMapping("/data-access-request")
    public ResponseEntity<?> requestDataAccess(
        @RequestParam @Email String email,
        HttpServletRequest request
    ) {
        Map<String, Object> customerData = customerRightsService.getCustomerData(email);
        auditService.log("DATA_ACCESS_REQUEST", "Customer", email, null, null, request);
        return ResponseEntity.ok(customerData);
    }
}
```

```java
// service/CustomerRightsService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRightsService {

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    @Transactional
    public void processErasureRequest(String email, HttpServletRequest request) {

        // PDPP s.24 — anonymise personal data (do not delete orders — financial records)
        // We CANNOT delete orders (7-year legal requirement for financial records)
        // But we CAN anonymise the personal identifiers

        List<Order> orders = orderRepository.findByCustomerEmail(email);

        orders.forEach(order -> {
            // Replace personal data with anonymised values
            order.setCustomerName("ANONYMISED");
            order.setCustomerEmailEncrypted(null);
            order.setCustomerPhoneEncrypted(null);
            order.setCustomerEmail("anonymised-" + order.getId() + "@deleted.invalid");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });

        auditService.log("ERASURE_REQUEST_PROCESSED", "Customer", email,
            Map.of("ordersAnonymised", orders.size()),
            Map.of("status", "ANONYMISED", "processedAt", LocalDateTime.now()),
            request
        );

        log.info("Erasure request processed for {} — {} orders anonymised", email, orders.size());
    }

    public Map<String, Object> getCustomerData(String email) {
        List<Order> orders = orderRepository.findByCustomerEmail(email);
        return Map.of(
            "email", email,
            "ordersCount", orders.size(),
            "orders", orders.stream().map(o -> Map.of(
                "orderId", o.getId(),
                "date", o.getCreatedAt(),
                "amount", o.getAmount(),
                "currency", o.getCurrency(),
                "status", o.getStatus()
            )).toList(),
            "dataRetentionPeriod", "7 years from order date",
            "governingLaw", "PDPP 2025 — The Gambia"
        );
    }
}
```

---

### REQUIREMENT 4 — Data Breach Notification (PDPP s.38 — 72 hours)

PDPP 2025 requires notifying the Information Commission within 72 hours of discovering a breach.

```java
// service/DataBreachService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class DataBreachService {

    private final AuditService auditService;
    private final JavaMailSender mailSender;

    @Value("${notification.dpa-email:informationcommission@gambia.gov.gm}")
    private String dpaEmail;

    @Value("${notification.admin-email}")
    private String adminEmail;

    /**
     * Call this immediately when a potential breach is detected.
     * PDPP 2025 s.38 — notify DPA within 72 hours.
     */
    public void reportBreach(String description, String affectedData,
                              int estimatedAffectedUsers, HttpServletRequest request) {

        LocalDateTime breachTime = LocalDateTime.now();

        // 1. Log internally first
        auditService.log("DATA_BREACH_DETECTED", "System", null,
            null,
            Map.of(
                "description", description,
                "affectedData", affectedData,
                "estimatedAffectedUsers", estimatedAffectedUsers,
                "detectedAt", breachTime
            ),
            request
        );

        // 2. Alert admin immediately
        sendBreachAlert(adminEmail, "[URGENT] Data Breach Detected — lumoo.my",
            buildAdminBreachEmail(description, affectedData, estimatedAffectedUsers));

        // 3. Notify Gambia Information Commission (within 72 hours — PDPP s.38)
        sendBreachAlert(dpaEmail, "Data Breach Notification — lumoo.my (PDPP 2025)",
            buildDPANotificationEmail(description, affectedData,
                                      estimatedAffectedUsers, breachTime));

        log.error("DATA BREACH REPORTED: {} — {} users affected", description, estimatedAffectedUsers);
    }

    private String buildDPANotificationEmail(String description, String affectedData,
                                              int users, LocalDateTime detected) {
        return """
            DATA BREACH NOTIFICATION
            Under Personal Data Protection and Privacy Act 2025, Section 38
            
            Data Controller: Lumoo E-Commerce (lumoo.my)
            Contact: privacy@lumoo.my
            
            Breach Details:
            - Detected at: %s
            - Description: %s
            - Data affected: %s
            - Estimated affected users: %d
            
            Immediate actions taken:
            - Breach isolated and contained
            - Internal investigation commenced
            - This notification sent within 72-hour requirement
            
            Full incident report to follow within 30 days.
            """.formatted(detected, description, affectedData, users);
    }

    private void sendBreachAlert(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send breach notification to {}", to, e);
        }
    }
}
```

---

### REQUIREMENT 5 — Data Minimisation (PDPP s.8 — collect only what is needed)

```java
// Only collect data you actually use — PDPP principle of minimisation

// WRONG — collecting unnecessary data
public class CheckoutRequest {
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;    // ← do you need this for digital goods?
    private String customerNationality; // ← never needed
    private Date customerDOB;           // ← never needed
    private String customerIC;          // ← never needed for e-commerce
}

// CORRECT — only what is needed for fulfillment
public class CheckoutRequest {
    private String customerName;   // for addressing package/confirmation
    private String customerEmail;  // for order confirmation
    private String customerPhone;  // for delivery coordination only
    // Nothing else — PDPP s.8 compliant
}
```

---

### REQUIREMENT 6 — Data Retention Policy

```java
// service/DataRetentionService.java
// Run monthly via @Scheduled — clean up old data per PDPP guidelines

@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    // Run on 1st of every month at 2am
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void enforceRetentionPolicy() {

        // Financial records (orders) — keep 7 years (Gambia Companies Act requirement)
        // BUT anonymise personal identifiers after 2 years
        LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);

        List<Order> oldOrders = orderRepository
            .findByCreatedAtBeforeAndCustomerNameNot(twoYearsAgo, "ANONYMISED");

        oldOrders.forEach(order -> {
            order.setCustomerName("ANONYMISED");
            order.setCustomerEmailEncrypted(null);
            order.setCustomerPhoneEncrypted(null);
            order.setCustomerEmail("anonymised-" + order.getId() + "@expired.invalid");
            orderRepository.save(order);
        });

        // Audit logs — keep 5 years (legal evidence requirement)
        // Do NOT delete audit logs — they are legal evidence

        // Failed login attempts — keep 90 days only
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        // loginAttemptRepository.deleteByAttemptedAtBefore(ninetyDaysAgo);

        log.info("Retention policy enforced: {} orders anonymised", oldOrders.size());
    }
}
```

---

### REQUIREMENT 7 — New DB Migration for PDPP Compliance

```sql
-- V3__pdpp_compliance.sql

-- Add consent tracking to orders
ALTER TABLE orders
    ADD COLUMN privacy_accepted    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN terms_accepted      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN marketing_consent   BOOLEAN DEFAULT FALSE,
    ADD COLUMN consent_timestamp   TIMESTAMP NULL,
    ADD COLUMN consent_ip          VARCHAR(45);

-- Erasure requests tracking
CREATE TABLE erasure_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    requested_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP NULL,
    status          ENUM('PENDING','PROCESSING','COMPLETED') DEFAULT 'PENDING',
    reference_id    VARCHAR(36) UNIQUE NOT NULL,
    orders_affected INT DEFAULT 0,
    INDEX idx_erasure_email (email),
    INDEX idx_erasure_status (status)
) COMMENT 'PDPP 2025 s.24 — Right to erasure requests';

-- Data access requests
CREATE TABLE data_access_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fulfilled_at TIMESTAMP NULL,
    reference_id VARCHAR(36) UNIQUE NOT NULL,
    INDEX idx_dar_email (email)
) COMMENT 'PDPP 2025 s.23 — Right to access requests';

-- Data breach incident log
CREATE TABLE breach_incidents (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    detected_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description      TEXT NOT NULL,
    affected_data    TEXT,
    affected_users   INT DEFAULT 0,
    dpa_notified_at  TIMESTAMP NULL COMMENT 'Must be within 72 hours per PDPP s.38',
    status           ENUM('DETECTED','CONTAINED','RESOLVED') DEFAULT 'DETECTED',
    INDEX idx_breach_detected (detected_at)
) COMMENT 'PDPP 2025 s.38 — Breach notification log';
```

---

### REQUIREMENT 8 — New API Endpoints for PDPP Compliance

```
# Customer rights endpoints (public — no auth needed)
POST /api/customer-rights/erasure-request?email=xxx    - Right to erasure
POST /api/customer-rights/data-access-request?email=xxx - Right to access

# Legal pages (public)
GET  /api/legal/privacy-policy    - Privacy policy (JSON)
GET  /api/legal/terms             - Terms & conditions (JSON)

# Admin breach reporting (JWT auth required)
POST /api/admin/breach/report     - Report data breach (triggers DPA notification)
GET  /api/admin/breach/incidents  - List breach incidents
GET  /api/admin/erasure-requests  - List pending erasure requests
PUT  /api/admin/erasure-requests/{id}/process - Process erasure request
```

---

### REQUIREMENT 9 — application.properties additions

```properties
# PDPP 2025 — Data Protection
notification.dpa-email=informationcommission@gambia.gov.gm
notification.admin-email=${ADMIN_EMAIL}
data.retention.orders-anonymise-years=2
data.retention.financial-keep-years=7
data.retention.audit-keep-years=5
data.retention.login-attempts-days=90

# Email (for breach notifications + order confirmations)
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

### REQUIREMENT 10 — .env additions

```env
# Email (for order confirmations + breach notifications)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Admin
ADMIN_EMAIL=admin@lumoo.my
```

---

### PDPP 2025 Compliance Checklist

**Lawful Basis & Consent**
- [ ] Privacy Policy accessible at /api/legal/privacy-policy
- [ ] Terms & Conditions accessible at /api/legal/terms
- [ ] Checkout requires privacy_policy_accepted = true
- [ ] Checkout requires terms_accepted = true
- [ ] Marketing consent is OPTIONAL (separate checkbox)
- [ ] Consent timestamp + IP recorded with every order

**Data Subject Rights**
- [ ] POST /api/customer-rights/erasure-request works
- [ ] Erasure anonymises data, does NOT delete orders (financial records)
- [ ] POST /api/customer-rights/data-access-request returns customer data
- [ ] Erasure processed within 30 days (tracked in erasure_requests table)

**Data Breach**
- [ ] DataBreachService.reportBreach() implemented
- [ ] Breach notification emails configured
- [ ] DPA email (informationcommission@gambia.gov.gm) in config
- [ ] breach_incidents table in DB
- [ ] Admin can log and track breach incidents

**Data Minimisation & Retention**
- [ ] Only name, email, phone collected at checkout
- [ ] DataRetentionService @Scheduled job exists
- [ ] Orders anonymised after 2 years (personal data)
- [ ] Financial records (amounts, items) kept 7 years
- [ ] Audit logs kept 5 years
- [ ] Login attempts purged after 90 days

**Security (supports PDPP)**
- [ ] Customer email/phone encrypted in DB (AES-256-GCM)
- [ ] All API over HTTPS only
- [ ] Audit trail for all data processing activities

---

## Clean Code — Final Checklist for Claude Code

Before considering any class complete, verify:

**Structure**
- [ ] Package structure is domain-based (domain/product/, domain/order/, etc.)
- [ ] Each class has ONE clear responsibility
- [ ] Controllers are thin — max 15 lines per method, no business logic
- [ ] Services contain all business logic — no HTTP objects inside service
- [ ] Repositories contain queries only — no business logic
- [ ] DTOs are separate from JPA Entities — never return Entity from controller
- [ ] Custom exceptions exist for every error type (ProductNotFoundException, etc.)
- [ ] ApiResponse<T> wrapper used on every endpoint response
- [ ] Dev and Prod application profiles are separated

**Clean Code Rules**
- [ ] No hardcoded URLs, timeouts, or magic numbers — use @Value or @ConfigurationProperties
- [ ] No N+1 queries — use JOIN FETCH where collections are loaded
- [ ] @Transactional on Service methods only — never on Controller
- [ ] @Valid on all @RequestBody parameters in controllers
- [ ] No string concatenation in queries — JPA parameterised only
- [ ] All exceptions caught specifically — no bare `catch (Exception e) {}`
- [ ] Every caught exception is logged before re-throwing or handling
- [ ] Method names are verbs (createOrder, findActiveProducts, markOrderPaid)
- [ ] Class names are nouns (OrderService, ProductRepository, CheckoutRequest)
- [ ] No single-letter variable names except loop counters (i, j)

**Testing**
- [ ] At least one unit test per service method
- [ ] Happy path tested for: checkout, markOrderPaid, getAllProducts
- [ ] Error path tested for: insufficient stock, invalid input, duplicate webhook
- [ ] WebhookController tested for valid and invalid signatures
- [ ] DataEncryptor tested: encrypt → decrypt → equals original
- [ ] Tests do NOT call real ModemPay API — use Mockito mocks
- [ ] Tests run with `./mvnw test` without any external dependencies

**The Litmus Test — Ask These Before Submitting:**
- Can a new developer understand what each class does in 30 seconds?
- If I delete domain/product/, does anything in domain/order/ break?
- Can I test OrderService without starting a real database?
- Does my controller have any business logic in it? (Should be: No)
- Does my GlobalExceptionHandler catch every custom exception?