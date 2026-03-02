# Microservice CI/CD Project — Learning Notes

> Personal study notes. Documents everything built step by step, with the reasoning behind each decision, errors encountered, and how they were fixed.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Phase 1 — Eureka Server + Jenkins CI/CD Pipeline](#2-phase-1--eureka-server--jenkins-cicd-pipeline)
3. [Phase 2 — product-service: REST API](#3-phase-2--product-service-rest-api)
4. [Phase 2 (cont.) — Bean Validation + Global Exception Handler](#4-phase-2-cont--bean-validation--global-exception-handler)
5. [Phase 2 (cont.) — Swagger / OpenAPI Documentation](#5-phase-2-cont--swagger--openapi-documentation)
6. [Phase 3 — SLF4J Logging + MDC Filter + Logback](#6-phase-3--slf4j-logging--mdc-filter--logback)
7. [Phase 4 — Spring Boot Actuator + Custom Health Indicator](#7-phase-4--spring-boot-actuator--custom-health-indicator)
8. [Phase 5 — Feign Client + Resilience4j](#phase-5--feign-client--resilience4j)
9. [Phase 8 — HandlerInterceptor + AOP](#phase-8--handlerinterceptor--aop)
10. [Phase 9 — Config Server + Eureka + API Gateway](#phase-9--config-server--eureka--api-gateway)
11. [Phase 6 — Kafka Async Messaging](#phase-6--kafka-async-messaging-assignment-7)
12. [Phase 10 — Docker Compose](#phase-10--docker-compose-assignment-12)
13. [CI/CD Deep Dive — Jenkins Pipeline in Detail](#cicd-deep-dive--jenkins-pipeline-in-detail)
14. [Complete Application Flow](#complete-application-flow)
15. [What to Use When — Decision Guide](#what-to-use-when--decision-guide)
16. [Errors & Fixes Log](#errors--fixes-log)
17. [Key Concepts Cheatsheet](#key-concepts-cheatsheet)

---

## 1. Project Overview

**What we're building:** A microservices system that covers the full developer journey — REST APIs, CI/CD pipeline, service communication, resilience, observability, and production deployment.

**Tech stack:**
- Java 17 + Spring Boot 3.4.3
- Maven (Maven Wrapper `mvnw`)
- Docker + Docker-in-Docker (Jenkins)
- Jenkins (running in Docker)
- GitHub (source control + PR-based branching)

**Final project structure:**
```
microservice-cicd-project/
├── docker-compose.yml          ← Starts ALL services with one command
├── eureka-server/              ← Service registry               :8761
├── config-server/              ← Centralised config             :8888
├── api-gateway/                ← Single entry point             :8080
├── product-service/            ← Products REST API              :8082
├── order-service/              ← Orders REST API + Feign        :8083
├── payment-service/            ← Kafka consumer                 :8084
└── NOTES.md                    ← This file
```

**Port summary:**
| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | Single entry point for all client traffic |
| eureka-server | 8761 | Service registry — every service registers here |
| config-server | 8888 | Serves config files to all services at startup |
| product-service | 8082 | Products CRUD REST API |
| order-service | 8083 | Orders API — calls product-service via Feign |
| payment-service | 8084 | Kafka consumer — processes payments asynchronously |
| kafka | 9092 | Message broker — carries OrderPlacedEvent |

**Git branching strategy:**
- `main` — production-ready code
- `develop` — integration branch
- `release/vN` — release branches (cut from main, merged back)
- `feature/*` — feature branches (merged into develop via PR)

---

## 2. Phase 1 — Eureka Server + Jenkins CI/CD Pipeline

### What was built
- **Eureka Server** — Spring Cloud Netflix service registry. Microservices register themselves here so they can discover each other by name instead of hardcoded IP.
- **Jenkins pipeline** — automated CI/CD triggered on push to GitHub.

### Eureka Server — `application.properties`
```properties
spring.application.name=eureka-server
server.port=8761
# Don't register the server with itself
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### Eureka Server — `EurekaServerApplication.java`
```java
@SpringBootApplication
@EnableEurekaServer        // ← turns this app into the registry
public class EurekaServerApplication { ... }
```

### Jenkins Pipeline — `Jenkinsfile` (final working version)
The pipeline:
1. **Checkout** — pulls code from GitHub
2. **Build (Maven)** — compiles and packages the JAR
3. **Build Docker Image** — builds and tags the image
4. **Deploy to TEST / STAGING / PRODUCTION** — deploys based on branch name

**Key pipeline concepts learned:**
- `agent any` — run on any available Jenkins agent
- `environment {}` — define pipeline-wide variables
- `when { branch 'X' }` — conditional stages
- `steps { sh '...' }` — run shell commands

### The Docker volume mounting problem (the hard one)
Jenkins runs inside Docker. When it runs `docker run -v /path:/workspace`, the path must exist **on the Docker host**, not inside the Jenkins container. The mounted volume appeared empty inside Maven container because of how Docker-in-Docker works.

**Final fix — tar streaming approach:**
```bash
# Stream source files INTO the container via stdin, run build, stream target/ back out
tar -czf - . | docker run --rm -i maven:3.9.9-eclipse-temurin-17 sh -c \
  "tar -xzf - 1>&2 && mvn clean package -DskipTests 1>&2 && tar -czf - target/" \
  | tar -xzf - -C ./
```
**Why `1>&2` on Maven?** Maven writes to stdout (`INFO`, `ERROR` lines). Those polluted the stdout pipe that was carrying the tar stream back. Redirecting Maven's stdout to stderr keeps the pipe clean for just the tar data.

---

## 3. Phase 2 — product-service: REST API

### Assignment 1 — 5 CRUD Endpoints

**Goal:** Build a complete REST API for managing products, following HTTP standards correctly.

#### Project structure
```
product-service/src/main/java/com/example/product_service/
├── ProductServiceApplication.java
├── Controller/
│   └── ProductController.java     ← HTTP layer: handles requests, returns responses
├── service/
│   └── ProductService.java        ← Business logic layer: does the actual work
├── model/
│   └── Product.java               ← Data model
├── exception/
│   ├── ProductNotFoundException.java
│   └── GlobalExceptionHandler.java
├── config/
│   └── OpenApiConfig.java
└── filter/
    └── MdcFilter.java
```

#### Why separate Controller and Service?
- **Controller** — knows about HTTP (status codes, request/response). Should NOT contain business logic.
- **Service** — knows about business rules. Should NOT know anything about HTTP.
- If you ever switch from REST to gRPC, you only rewrite the controller. Service stays the same.

#### In-memory store (no database yet)
```java
// ProductService.java
private final Map<Long, Product> productStore = new HashMap<>();
private long idCounter = 1;
```
Using a `HashMap` as a stand-in for a database. `idCounter` auto-increments like a DB sequence.

#### The 5 endpoints

| Method | URL | Status | What it does |
|--------|-----|--------|--------------|
| GET | `/api/products` | 200 | Get all products |
| GET | `/api/products/{id}` | 200 / 404 | Get one product |
| POST | `/api/products` | **201** | Create product |
| PUT | `/api/products/{id}` | 200 / 404 | Replace product |
| DELETE | `/api/products/{id}` | **204** | Delete product |

#### Critical REST rules
- `POST` must return **201 Created**, not 200 OK
- `DELETE` must return **204 No Content** (no body)
- Use `ResponseEntity` to control the exact status code

```java
// WRONG — returns 200
return ResponseEntity.ok(product);

// CORRECT — returns 201
return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);

// CORRECT — returns 204 (no body)
return ResponseEntity.noContent().build();
```

#### Bug fixed: returning wrong object after POST
```java
// WRONG — returns the input object (no id assigned yet at this point)
Product createdProduct = productService.createProduct(product);
return ResponseEntity.status(HttpStatus.CREATED).body(product);   // ← WRONG object

// CORRECT — return the object that came BACK from the service (has the id)
return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct); // ← correct
```

#### Bug fixed: updateProduct not preserving the id
```java
// WRONG — id comes from URL path, NOT from the request body
// body may have no id or wrong id
public Product updateProduct(Long id, Product product) {
    productStore.put(id, product);   // ← product.id might be null!
    return product;
}

// CORRECT — explicitly set the id from the path variable
public Product updateProduct(Long id, Product product) {
    product.setId(id);              // ← always use the URL path id
    productStore.put(id, product);
    return product;
}
```

---

## 4. Phase 2 (cont.) — Bean Validation + Global Exception Handler

### Assignment 2 — Input Validation + Structured Error Responses

**Goal:** Reject invalid input before it reaches business logic. Return clean, structured error responses instead of Spring's default HTML error page.

#### Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Product model — validation annotations
```java
public class Product {
    private Long id;                    // no validation — auto-generated

    @NotBlank                           // must not be null or empty string
    private String name;

    @NotNull                            // must be provided
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    // ...
}
```

#### How validation is triggered
```java
// In controller — @Valid tells Spring to run the validation constraints
@PostMapping
public ResponseEntity<Product> createProduct(@RequestBody @Valid Product product) { ... }
//                                                         ↑
//                            Without @Valid, the annotations on Product are IGNORED
```

**What happens when validation fails:** Spring throws `MethodArgumentNotValidException` automatically. Without a handler, it returns a 400 with a confusing default body.

#### GlobalExceptionHandler — `@RestControllerAdvice`
```java
@RestControllerAdvice   // applies to ALL controllers in the app
public class GlobalExceptionHandler {

    // Handles ProductNotFoundException → 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("status", 404, "message", ex.getMessage()));
    }

    // Handles @Valid failures → 400 with field-level errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("status", 400, "errors", fieldErrors));
    }
}
```

#### What the error responses look like

**404 — Product not found:**
```json
{ "status": 404, "message": "Product Not found with id: 999" }
```

**400 — Validation failed:**
```json
{
  "status": 400,
  "errors": {
    "name": "must not be blank",
    "price": "Price must be greater than 0"
  }
}
```

#### ProductNotFoundException
```java
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product Not found with id: " + id);
    }
}
```
Extends `RuntimeException` — unchecked, no need to declare it with `throws`.

---

## 5. Phase 2 (cont.) — Swagger / OpenAPI Documentation

### Assignment 3 — Interactive API Documentation

**Goal:** Auto-generate a live, interactive API documentation UI from the code annotations.

#### What Swagger gives you
- Browser UI at `/swagger-ui.html` — test every endpoint without Postman
- Machine-readable spec at `/v3/api-docs` — frontend teams generate type-safe clients from it
- Always in sync with code — no manual docs to maintain

#### Dependency (critical: version must match Spring Boot version)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>   <!-- must match Spring Boot 3.x / Spring FW 6.x -->
</dependency>
```

#### Configuration — `OpenApiConfig.java`
```java
@OpenAPIDefinition(
    info = @Info(
        title = "Product Service API",
        version = "v1.0",
        description = "REST API for managing products in the e-commerce platform"
    )
)
@Configuration
public class OpenApiConfig {
    // No beans needed — the annotation is enough
}
```

#### `application.properties` settings
```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.operations-sorter=method
springdoc.packages-to-scan=com.example.product_service
```

#### Annotations used

| Annotation | Where | What it does |
|---|---|---|
| `@OpenAPIDefinition` | Config class | Sets global title, version, description |
| `@Tag` | Controller class | Groups all endpoints under a named section |
| `@Operation` | Controller method | Summary + description for one endpoint |
| `@ApiResponse` / `@ApiResponses` | Controller method | Documents each possible HTTP response |
| `@Parameter` | Method parameter | Describes `@PathVariable` / `@RequestParam` |
| `@Schema` | Model class / field | Documents field descriptions, examples, constraints |

#### Example — fully documented endpoint
```java
@GetMapping("/{id}")
@Operation(summary = "Get product by ID", description = "Returns 404 if not found")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Product found",
        content = @Content(schema = @Schema(implementation = Product.class))),
    @ApiResponse(responseCode = "404", description = "Product not found",
        content = @Content(schema = @Schema(hidden = true)))
})
public ResponseEntity<Product> getProductById(
        @Parameter(description = "Unique ID", example = "1", required = true)
        @PathVariable Long id) { ... }
```

#### Version incompatibility error (ERROR 13)

**Error:**
```
Fetch error response status is 500 /v3/api-docs
NoSuchMethodError: ControllerAdviceBean.<init>(Object)
```

**Root cause:**
```
Spring Boot 4.0.3  →  Spring Framework 7.x  (new ControllerAdviceBean API)
springdoc 2.3.0    →  built for Spring FW 6.x (calls old constructor)
= NoSuchMethodError at runtime
```

**Fix:** Downgrade Spring Boot to **3.4.3** (uses Spring FW 6.x, compatible with springdoc 2.x)

**Compatibility table:**

| Spring Boot | Spring Framework | springdoc version |
|---|---|---|
| 3.4.x ✅ | 6.x | 2.8.x |
| 4.0.x | 7.x | No stable springdoc release yet |

**Also fixed in `pom.xml`:**
- `spring-boot-starter-webmvc` → `spring-boot-starter-web` (correct artifact name)
- `spring-boot-starter-webmvc-test` → `spring-boot-starter-test` (correct artifact name)

---

## 6. Phase 3 — SLF4J Logging + MDC Filter + Logback

### Assignment 4 — Production-Quality Logging

**Goal:** Add meaningful logs to controller and service, auto-attach request context to every log line, write to files with rotation, and output JSON in production for ELK.

#### How Java logging works (the stack)
```
Your Code
    ↓  calls
SLF4J API          ← the interface YOUR code uses (log.info, log.debug, etc.)
    ↓  delegates to
Logback            ← the actual implementation (Spring Boot's default)
    ↓  writes to
Console / File / ELK
```

**Key rule:** Always import from `org.slf4j` — NEVER from `ch.qos.logback` directly. This keeps your code portable (can swap Logback for Log4j2 without any code changes).

#### Creating a logger — one per class
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductController {
    // static final — one shared instance per class, created once
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    //                                                         ↑ class name = logger name
    // logger name = "com.example.product_service.Controller.ProductController"
    // this is what you configure in logback.xml / application.properties
}
```

#### Log levels — which to use when

| Level | When to use | Example |
|-------|-------------|---------|
| `TRACE` | Every line of code — method entry/exit, loop iterations | `log.trace("Entering loop i={}", i)` |
| `DEBUG` | Internal state — cache hit/miss, what branch was taken | `log.debug("Cache miss for id={}", id)` |
| `INFO` | Business events — request received, record saved | `log.info("Product created: id={}", id)` |
| `WARN` | Recoverable unexpected events — 404, retry | `log.warn("Product not found: id={}", id)` |
| `ERROR` | Errors that need attention — exceptions | `log.error("Payment failed: {}", e.getMessage(), e)` |

**Rule:** Set level to X → shows X and everything HIGHER. Setting INFO shows INFO, WARN, ERROR but not DEBUG, TRACE.

#### {} placeholder — WHY it matters (performance)
```java
// BAD — string is ALWAYS built, even if DEBUG is disabled
log.debug("Product: " + product.toString() + " count: " + count);

// GOOD — toString() is only called IF debug level is enabled
log.debug("Product: {} count: {}", product, count);
```

#### Logging in ProductController
```java
// INFO for business events (request in, result out)
log.info("Request received: GET /api/products/{}", id);
log.info("Found product: id={}, name={}", product.getId(), product.getName());

// WARN when something expected but not normal happens
log.warn("Product not found: id={}", id);

// Always pass the exception object as the LAST argument (prints full stack trace)
log.error("Unexpected error processing id={}", id, e);
```

#### Logging in ProductService
```java
// DEBUG for internal mechanics — only visible when you need to diagnose
log.debug("Fetching all products from store — current count: {}", productStore.size());
log.debug("Cache miss — no product found for id={}", id);
log.debug("Saved product to store: id={}, name={}, price={}", ...);
```

**Why controller uses INFO and service uses DEBUG:**
- Controller logs are business events — you always want to see them in production
- Service logs are implementation details — too noisy for production, useful when debugging

---

### MDC Filter — `MdcFilter.java`

**The problem MDC solves:**

In a server handling 100 concurrent requests, your logs look like:
```
INFO - Processing order
INFO - Fetching product
ERROR - Payment failed
```
Whose order? Which request? **Impossible to tell.**

With MDC, EVERY log line for a request automatically includes the request ID:
```
INFO  [abc-1234] [POST] [/api/products] ProductController - Request received
DEBUG [abc-1234] [POST] [/api/products] ProductService   - Saved product to store
INFO  [abc-1234] [POST] [/api/products] ProductController - Product created: id=1
```
Now you can search for `abc-1234` and see the complete trace for one request across all classes.

**MDC = thread-local key-value store.** Values put into MDC are automatically available to ALL log calls on the same thread, for the lifetime of that thread's work.

```java
@Component
@Order(1)   // run FIRST, before all other filters
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Use caller's X-Request-Id if present (distributed tracing across services)
        // Otherwise generate a new short random ID
        String requestId = httpRequest.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("requestId", requestId);         // ← now %X{requestId} works in logback pattern
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);   // process the request
        } finally {
            MDC.clear();   // ← CRITICAL — threads are reused from pool!
                           //   without this, next request on same thread gets old values
        }
    }
}
```

---

### `logback-spring.xml` — Full Logging Configuration

Lives in `src/main/resources/logback-spring.xml`. Spring Boot reads this automatically.

**Why `logback-spring.xml` instead of `logback.xml`?**
The `-spring` suffix lets Logback use Spring's `<springProfile>` tag — allowing different config per environment.

```xml
<configuration>

    <!-- LOCAL/DEFAULT profile — human-readable, colour console + file output -->
    <springProfile name="default,local">

        <!-- CONSOLE appender — writes to stdout -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <!-- %X{key} reads from MDC -->
                <pattern>%d{HH:mm:ss.SSS} [%X{requestId}] [%X{method}] [%X{uri}] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <!-- Rolling file — rotates daily, keeps 7 days, max 200MB total -->
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/product-service.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/product-service.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>7</maxHistory>
                <totalSizeCap>200MB</totalSizeCap>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{requestId}] [%X{method}] [%X{uri}] %-5level %logger - %msg%n</pattern>
            </encoder>
        </appender>

        <!-- ERROR-only file — only ERROR lines written here (for alerts/monitoring) -->
        <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/product-service-errors.log</file>
            <filter class="ch.qos.logback.classic.filter.LevelFilter">
                <level>ERROR</level>
                <onMatch>ACCEPT</onMatch>
                <onMismatch>DENY</onMismatch>
            </filter>
            <!-- ... rolling policy ... -->
        </appender>

        <!-- Your app: DEBUG level -->
        <logger name="com.example.product_service" level="DEBUG"/>
        <!-- Framework code: silence it -->
        <logger name="org.springframework" level="WARN"/>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>

    </springProfile>

    <!-- PROD profile — JSON for ELK/Splunk. Activate: SPRING_PROFILES_ACTIVE=prod -->
    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="ch.qos.logback.classic.encoder.JsonEncoder"/>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>

</configuration>
```

#### Log pattern tokens reference

| Token | Output |
|-------|--------|
| `%d{HH:mm:ss.SSS}` | Timestamp: `15:22:01.450` |
| `%X{requestId}` | MDC value: `abc-1234` |
| `%-5level` | Log level, padded to 5 chars: `INFO `, `DEBUG`, `WARN ` |
| `%logger{36}` | Logger name, max 36 chars: `c.e.p.Controller.ProductController` |
| `%msg` | The actual log message |
| `%n` | Newline |

#### Example log output (local profile)
```
15:35:18.626 [fe5b3a4f] [POST] [/api/products] INFO  c.e.p.Controller.ProductController - Request received: POST /api/products — name=Laptop, price=75000.0
15:35:18.628 [fe5b3a4f] [POST] [/api/products] DEBUG c.e.p.service.ProductService       - Saved product to store: id=1, name=Laptop, price=75000.0
15:35:18.628 [fe5b3a4f] [POST] [/api/products] INFO  c.e.p.Controller.ProductController - Product created: id=1, name=Laptop
```

**Log files written to:**
- `logs/product-service.log` — all logs (rotates daily, 7 days)
- `logs/product-service-errors.log` — ERROR only (rotates daily, 30 days)

---

## 7. Phase 4 — Spring Boot Actuator + Custom Health Indicator

### Assignment 5 — Production Monitoring Endpoints

**Goal:** Add built-in monitoring to the running app — health checks, metrics, runtime log level changes — without writing any HTTP controllers.

#### Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
One dependency — 15+ production endpoints appear automatically.

#### Configuration in `application.properties`
```properties
# Expose only what's needed (default: only health is exposed)
management.endpoints.web.exposure.include=health,info,loggers,metrics,mappings,beans

# Show full component-level breakdown in /actuator/health
management.endpoint.health.show-details=always

# Enable Kubernetes liveness/readiness probes
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true

# Required since Spring Boot 2.6+ to show info.* from properties
management.info.env.enabled=true

# App metadata shown at /actuator/info
info.app.name=product-service
info.app.version=1.0.0
info.app.description=REST API for managing products
info.app.author=Ankur Rana
```

#### All exposed endpoints and what they return

| Endpoint | Method | What you get |
|---|---|---|
| `/actuator/health` | GET | Overall UP/DOWN + component breakdown |
| `/actuator/health/liveness` | GET | Is the JVM alive? (Kubernetes liveness probe) |
| `/actuator/health/readiness` | GET | Ready to serve traffic? (Kubernetes readiness probe) |
| `/actuator/info` | GET | App name, version, description, author |
| `/actuator/metrics` | GET | List of all available metric names |
| `/actuator/metrics/{name}` | GET | Value of a specific metric |
| `/actuator/loggers` | GET | All loggers and their configured levels |
| `/actuator/loggers/{name}` | GET/POST | Read or **change** a log level at runtime |
| `/actuator/mappings` | GET | All registered `@RequestMapping` routes |
| `/actuator/beans` | GET | All Spring beans in the application context |

#### `/actuator/health` response — what it looks like
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": { "total": 524287995904, "free": 330266742784, "threshold": 10485760 }
    },
    "livenessState":  { "status": "UP" },
    "readinessState": { "status": "UP" },
    "ping":           { "status": "UP" },
    "productStore": {
      "status": "UP",
      "details": {
        "productCount": 0,
        "maxProducts": 1000,
        "storeType": "In-memory HashMap",
        "status": "Operational"
      }
    }
  }
}
```

#### `/actuator/metrics/http.server.requests` — HTTP request metrics
```json
{
  "name": "http.server.requests",
  "baseUnit": "seconds",
  "measurements": [
    { "statistic": "COUNT",      "value": 2.0 },
    { "statistic": "TOTAL_TIME", "value": 0.0112 },
    { "statistic": "MAX",        "value": 0.0058 }
  ],
  "availableTags": [
    { "tag": "status", "values": ["200", "404"] },
    { "tag": "uri",    "values": ["/api/products", "/api/products/{id}"] },
    { "tag": "method", "values": ["GET", "POST", "DELETE"] }
  ]
}
```
Filter by tag: `GET /actuator/metrics/http.server.requests?tag=status:500` — only 500 errors.

---

### Custom Health Indicator — `ProductStoreHealthIndicator.java`

**Why write a custom one?**
Actuator's built-in health checks cover disk, ping, and DB connection pool. But it knows nothing about YOUR application's components. You write a `HealthIndicator` to tell Actuator how to check YOUR stuff.

**Real-world uses:**
- Check if a database query returns results
- Ping an external payment API
- Verify a Redis cache is reachable
- Check that a required config file exists

**How Spring discovers it:** Any `@Component` that implements `HealthIndicator` is automatically picked up. The name in the health response is derived from the class name — `ProductStoreHealthIndicator` → `"productStore"`.

```java
@Component
public class ProductStoreHealthIndicator implements HealthIndicator {

    private static final int MAX_PRODUCTS = 1000;
    private final ProductService productService;

    @Override
    public Health health() {
        try {
            int productCount = productService.getAllProducts().size();

            if (productCount >= MAX_PRODUCTS) {
                // Return DOWN with details explaining why
                return Health.down()
                        .withDetail("status", "Store at capacity")
                        .withDetail("productCount", productCount)
                        .withDetail("maxProducts", MAX_PRODUCTS)
                        .withDetail("action", "Archive or delete old products")
                        .build();
            }

            // Return UP with useful details
            return Health.up()
                    .withDetail("productCount", productCount)
                    .withDetail("maxProducts", MAX_PRODUCTS)
                    .withDetail("storeType", "In-memory HashMap")
                    .withDetail("status", "Operational")
                    .build();

        } catch (Exception e) {
            // If the check itself crashes → DOWN
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

**Key API:**
```java
Health.up()               // status: UP
Health.down()             // status: DOWN
Health.down(exception)    // status: DOWN, includes exception details
.withDetail("key", value) // adds to the "details" object in response
.build()                  // produces the Health object
```

**How overall status is calculated:**
- If ALL components are UP → overall `"status": "UP"`
- If ANY component is DOWN → overall `"status": "DOWN"` → HTTP response becomes `503 Service Unavailable`
- Kubernetes uses this: if `/actuator/health` returns 503, it stops routing traffic to the pod

---

### Runtime log level change — the killer feature

**Use case:** Bug reported in production at 2 AM. You need DEBUG logs to diagnose it. Without Actuator, you'd have to modify config, rebuild, redeploy, restart — 10–30 minutes of downtime.

With Actuator — **takes 2 seconds, zero downtime:**

```bash
# 1. Check current level
GET /actuator/loggers/com.example.product_service
→ { "configuredLevel": "INFO", "effectiveLevel": "INFO" }

# 2. Enable DEBUG — INSTANT, no restart
POST /actuator/loggers/com.example.product_service
Body: { "configuredLevel": "DEBUG" }

# 3. Reproduce the bug — now DEBUG logs appear
# 4. Diagnose and fix

# 5. Reset back to INFO
POST /actuator/loggers/com.example.product_service
Body: { "configuredLevel": null }
```

The change is **in-memory only** — app restart will restore the original level from config.

---

### Why NOT expose all endpoints publicly

`/actuator/env` — exposes ALL environment variables including **secrets, passwords, tokens**
`/actuator/heapdump` — downloads full JVM memory — attacker reads passwords from heap
`/actuator/beans` — shows internal architecture details
`/actuator/shutdown` — can kill your running app

**Production best practice:**
```properties
# Option 1 — run actuator on a separate internal-only port
management.server.port=8081
# Block port 8081 on the firewall — only Prometheus/monitoring can reach it

# Option 2 — expose only what's absolutely needed
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.show-details=when_authorized
```

---

## 8. Phase 6 — order-service + Feign Client

### Assignment 6 — Inter-Service Communication

**Goal:** Build a second microservice (`order-service`) that calls `product-service` via Feign Client to validate products before placing orders.

#### Architecture
```
POST /api/orders  →  order-service (:8083)
                           │
                           │  Feign: GET /api/products/{id}
                           ▼
                     product-service (:8082)
                           │
                           └→ returns ProductDto (or 404)
```

#### New project structure
```
order-service/
├── model/
│   ├── Order.java              ← id, customerName, productId, productName,
│   │                              quantity, unitPrice, totalPrice, status
│   └── OrderStatus.java        ← enum: PENDING / CONFIRMED / REJECTED
├── dto/
│   ├── CreateOrderRequest.java ← what the client sends (customerName, productId, quantity)
│   └── ProductDto.java         ← what we read back from product-service
├── client/
│   └── ProductClient.java      ← @FeignClient interface — declares the HTTP call
├── service/
│   └── OrderService.java       ← calls Feign, validates, calculates price, saves
├── controller/
│   └── OrderController.java    ← GET /api/orders, GET /api/orders/{id}, POST /api/orders
└── exception/
    ├── OrderNotFoundException.java
    ├── ProductNotAvailableException.java
    └── GlobalExceptionHandler.java
```

#### Spring Cloud dependency setup
```xml
<!-- pom.xml — Spring Cloud BOM manages feign version automatically -->
<properties>
    <spring-cloud.version>2024.0.0</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>   <!-- imports ALL spring-cloud versions into this pom -->
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <!-- no version needed — managed by BOM above -->
</dependency>
```

#### Enable Feign in the application class
```java
@SpringBootApplication
@EnableFeignClients   // scans this package for @FeignClient interfaces → registers them as beans
public class OrderServiceApplication { ... }
```

#### The Feign Client — `ProductClient.java`
```java
@FeignClient(name = "product-service", url = "${product.service.url}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);
}
```
**That's it. No implementation needed.** Spring generates a proxy class at startup.
When you call `productClient.getProductById(1L)`, the proxy builds and sends:
```
GET http://localhost:8082/api/products/1
```
...deserialises the JSON response into `ProductDto`, and returns it.

#### Why `ProductDto` instead of importing `Product` from product-service

| Approach | Problem |
|---|---|
| Import `Product.java` from product-service | Tight coupling — changing product-service breaks order-service at compile time |
| Define own `ProductDto` in order-service | Loose coupling — each service owns its data model. If product-service adds new fields, order-service ignores them safely |

This is called **contract-based integration** — each service defines its own view of what it needs from the other service.

#### `CreateOrderRequest` DTO — why not use `Order` directly
```java
// Client sends ONLY what they know:
public class CreateOrderRequest {
    @NotBlank  String customerName;
    @NotNull   Long productId;
    @NotNull @Min(1)  Integer quantity;
}

// Server computes the rest:
// - id (auto-generated)
// - productName (fetched from product-service)
// - unitPrice (fetched from product-service)
// - totalPrice = unitPrice × quantity
// - status = CONFIRMED (after validation)
```

#### Order creation flow in `OrderService`
```java
public Order createOrder(CreateOrderRequest request) {
    // 1. Call product-service via Feign
    //    → If 404: FeignException.NotFound thrown automatically
    ProductDto product = productClient.getProductById(request.getProductId());

    // 2. Check availability
    if (!product.isAvailable()) {
        throw new ProductNotAvailableException(request.getProductId());
    }

    // 3. Calculate total and save
    double totalPrice = product.getPrice() * request.getQuantity();
    Order order = Order.builder()
            .id(idCounter++)
            .customerName(request.getCustomerName())
            .productName(product.getName())    // ← from product-service response
            .unitPrice(product.getPrice())     // ← from product-service response
            .totalPrice(totalPrice)            // ← calculated here
            .status(OrderStatus.CONFIRMED)
            .build();
    orderStore.put(order.getId(), order);
    return order;
}
```

#### Error handling for Feign in `GlobalExceptionHandler`
```java
// product-service returned 404 → Feign throws this
@ExceptionHandler(FeignException.NotFound.class)
public ResponseEntity<...> handleProductNotFound(FeignException.NotFound ex) {
    return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
}

// product-service is down / 500 / connection refused → Feign throws this
@ExceptionHandler(FeignException.class)
public ResponseEntity<...> handleFeignError(FeignException ex) {
    return ResponseEntity.status(502).body(Map.of(
        "message", "product-service unavailable",
        "upstreamStatus", ex.status()
    ));
}
```

**502 Bad Gateway** = your service is fine but an upstream dependency failed. This is the correct HTTP status code when a service you depend on is down.

#### Feign HTTP logging — see the actual request/response
```properties
# application.properties
feign.client.config.default.loggerLevel=BASIC
logging.level.com.example.order_service.client=DEBUG
```
With `BASIC` level you see:
```
DEBUG ProductClient - [ProductClient#getProductById] ---> GET http://localhost:8082/api/products/1
DEBUG ProductClient - [ProductClient#getProductById] <--- HTTP/1.1 200 (12ms)
```

#### Proven test results

| Test | Request | Response |
|---|---|---|
| Create product | `POST /api/products` → name=MacBook Pro, price=150000 | `201` id=1 |
| Place order | `POST /api/orders` → productId=1, qty=2 | `201` totalPrice=300000, status=CONFIRMED |
| Get all orders | `GET /api/orders` | `200` list with 1 order |
| Get by ID | `GET /api/orders/1` | `200` order details |
| Product not found | `POST /api/orders` → productId=999 | `404` product not found |
| Product unavailable | `POST /api/orders` → productId=2 (available=false) | `422` product not available |
| Validation error | `POST /api/orders` → empty body | `400` field errors |

#### `application.properties` key setting
```properties
server.port=8083
product.service.url=http://localhost:8082   # base URL for all Feign calls
```

---

## 9. Errors & Fixes Log

### Error 1 — `mvn` not recognized
```
mvn: The term 'mvn' is not recognized
```
**Fix:** Use the Maven Wrapper instead: `.\mvnw.cmd clean package`

---

### Error 2 — No goals specified
```
No goals have been specified for this build
```
**Fix:** `.\mvnw.cmd clean package -DskipTests`

---

### Error 3 — Docker build fails: `lstat /target: no such file or directory`
```
ERROR: failed to build: failed to solve: lstat /target: no such file or directory
```
**Fix:** Must run Maven build first to create `target/*.jar`, THEN build the Docker image.

---

### Error 4 — PowerShell syntax errors
```
&& is not a valid statement separator
```
**Fix:** PowerShell uses `;` not `&&`. Or use separate commands.

---

### Error 5 — No POM in Jenkins Docker container
```
The goal you specified requires a project to execute but there is no POM in this directory (/workspace/eureka-server)
```
**Root cause:** Docker-in-Docker volume mounting. When Jenkins (inside Docker) mounts `-v /path:/workspace`, the path must exist on the **Docker host**, not inside Jenkins. The Jenkins workspace path is a path inside the Jenkins container — which the outer Docker daemon doesn't have access to.

**Multiple attempts:**
1. Changed `$PWD` to `${WORKSPACE}` — didn't help
2. Added `ls -la` debug — files visible on host but NOT in container
3. Tried mounting only the subdirectory — still empty inside container
4. **Final fix:** `tar` streaming — bypass the volume mount entirely

```bash
tar -czf - . | docker run --rm -i maven:image sh -c \
  "tar -xzf - 1>&2 && mvn clean package -DskipTests 1>&2 && tar -czf - target/" \
  | tar -xzf - -C ./
```

---

### Error 6 — `gzip: stdin: not in gzip format` with tar streaming
```
gzip: stdin: not in gzip format
tar: Child died with signal 13
```
**Root cause:** Maven's `[INFO]` and `[ERROR]` output was going to stdout, polluting the tar binary stream.

**Fix:** Redirect Maven's stdout to stderr with `1>&2`:
```bash
mvn clean package -DskipTests 1>&2   # Maven logs go to stderr, not stdout
tar -czf - target/                    # Only the tar data goes to stdout
```

---

### Error 7 — POST returning 200 instead of 201
**Root cause:** Used `ResponseEntity.ok()` which always returns 200.

**Fix:**
```java
return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
```

---

### Error 8 — Java package name `Controller` (uppercase)
**Root cause:** Created the controller in a folder named `Controller` (capital C). Java package names should be all lowercase.

**Fix:** Renamed folder and package to `controller` (lowercase) in IDE.  
*(Note: In this project kept as `Controller` with capital C — it works but is non-standard)*

---

### Error 9 — `createProduct` returning wrong object (no id)
**Root cause:**
```java
Product createdProduct = productService.createProduct(product);
return ResponseEntity.status(HttpStatus.CREATED).body(product);  // returned input, not saved
```
**Fix:** Return `createdProduct` (which has the auto-generated id).

---

### Error 10 — `updateProduct` not preserving the id
**Root cause:** Request body `product` object had no `id` field (or wrong one). Putting it directly into the map caused id to be null.

**Fix:**
```java
product.setId(id);   // always use the id from the URL path
productStore.put(id, product);
```

---

### Error 11 — Dead code in `updateProduct` controller
Redundant null check after `productService.updateProduct()` that could never be reached.

**Fix:** Removed the dead code block.

---

### Error 12 — `GlobalExceptionHandler` was empty
The class existed but methods were not implemented — exceptions fell through to Spring's default handler.

**Fix:** Implemented `handleNotFound` and `handleValidation` methods.

---

### Error 13 — Swagger UI 500 error: `NoSuchMethodError ControllerAdviceBean`
```
Fetch error response status is 500 /v3/api-docs
java.lang.NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'
```
**Root cause:** Version mismatch between Spring Boot and springdoc.
- Spring Boot **4.0.3** uses Spring Framework **7.x** (new API)
- springdoc **2.3.0** was built for Spring Framework **6.x** (old API)

**Fix:** Downgraded `spring-boot-starter-parent` from `4.0.3` to `3.4.3`. Updated springdoc to `2.8.4`.

Also fixed wrong artifact names in `pom.xml`:
- `spring-boot-starter-webmvc` → `spring-boot-starter-web`
- `spring-boot-starter-webmvc-test` → `spring-boot-starter-test`

---

## 9. Key Concepts Cheatsheet

### HTTP Status Codes — when to use each

| Code | Name | Use when |
|------|------|----------|
| 200 | OK | GET succeeded, PUT succeeded |
| 201 | Created | POST succeeded — new resource created |
| 204 | No Content | DELETE succeeded — no body to return |
| 400 | Bad Request | Invalid input — validation failed |
| 404 | Not Found | Resource with that ID doesn't exist |
| 409 | Conflict | Duplicate — already exists |
| 500 | Internal Server Error | Unexpected exception |

### Spring annotations quick reference

| Annotation | Where | What it does |
|---|---|---|
| `@RestController` | Class | = `@Controller` + `@ResponseBody` on all methods |
| `@RequestMapping("/path")` | Class | Base URL prefix for all methods |
| `@GetMapping`, `@PostMapping`, etc. | Method | Map to HTTP method |
| `@PathVariable` | Parameter | Bind URL path segment `/products/{id}` |
| `@RequestBody` | Parameter | Deserialise JSON body to Java object |
| `@Valid` | Parameter | Trigger Bean Validation constraints |
| `@Service` | Class | Marks a Spring-managed service bean |
| `@Component` | Class | Generic Spring-managed bean |
| `@RestControllerAdvice` | Class | Handle exceptions thrown by ANY controller |
| `@ExceptionHandler(X.class)` | Method | Handle specific exception type |

### ResponseEntity patterns

```java
ResponseEntity.ok(data)                                    // 200
ResponseEntity.status(HttpStatus.CREATED).body(saved)      // 201
ResponseEntity.noContent().build()                         // 204
ResponseEntity.notFound().build()                          // 404
ResponseEntity.badRequest().body("error message")          // 400
ResponseEntity.status(HttpStatus.CONFLICT).body(message)   // 409
```

### SLF4J logging patterns

```java
// Correct — {} is lazy, no string concat
log.info("Processing product id={}, name={}", id, name);

// Log with exception — e as LAST arg prints full stack trace
log.error("Failed to process id={}: {}", id, e.getMessage(), e);

// Guard for very expensive log construction
if (log.isDebugEnabled()) {
    log.debug("Full dump: {}", buildExpensiveReport());
}
```

### MDC — what it gives you

Without MDC: 100 concurrent requests → log lines interleaved → impossible to trace one request.

With MDC filter: every log line tagged with `requestId` → search `requestId=abc-1234` → see full trace.

```java
MDC.put("requestId", uuid);   // in filter
// ... every log.info(), log.debug() etc. NOW includes requestId automatically ...
MDC.clear();                  // in finally block — ALWAYS
```

---

---

## Phase 7 — Resilience4j: Circuit Breaker + Retry (Assignment 8)

**Goal:** Protect `order-service` from cascading failures when `product-service` is down or slow. Without resilience patterns, one slow/dead downstream service will hold threads, exhaust connection pools, and eventually bring down the whole system.

**Files changed:**
- `order-service/pom.xml` — 3 new dependencies
- `order-service/src/main/resources/application.properties` — Resilience4j configuration
- `order-service/src/main/java/.../service/OrderService.java` — `@CircuitBreaker`, `@Retry`, fallback
- `order-service/src/main/java/.../exception/GlobalExceptionHandler.java` — `CallNotPermittedException` handler

### New dependencies (pom.xml)

```xml
<!-- The Resilience4j Spring Boot 3 autoconfiguration -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <!-- version managed by Spring Cloud BOM -->
</dependency>
<!-- Required: @CircuitBreaker and @Retry are AOP annotations -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<!-- Exposes CB metrics to /actuator/metrics -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
</dependency>
```

Why AOP? The annotations work by wrapping your bean in a proxy. Without AOP on the classpath, `@CircuitBreaker` and `@Retry` are silently ignored — no error, no protection.

### Circuit Breaker — how it works

Think of it like a fuse box. When too many failures happen, the circuit "trips" (opens) and **subsequent calls never reach the failing service** — they go straight to the fallback.

```
Normal (CLOSED):  call → product-service → response
Broken (OPEN):    call → fallback immediately (no network round-trip)
Testing (HALF_OPEN): a few test calls → if ok → CLOSED, if fail → OPEN again
```

**Configuration (`application.properties`):**
```properties
# Evaluate last 10 calls
resilience4j.circuitbreaker.instances.productService.sliding-window-size=10
# Open circuit if 50%+ of those 10 calls failed
resilience4j.circuitbreaker.instances.productService.failure-rate-threshold=50
# Need at least 5 calls before evaluating (avoid opening on very first failure)
resilience4j.circuitbreaker.instances.productService.minimum-number-of-calls=5
# Stay OPEN for 10 seconds, then go HALF_OPEN
resilience4j.circuitbreaker.instances.productService.wait-duration-in-open-state=10s
# In HALF_OPEN, allow 3 test calls through
resilience4j.circuitbreaker.instances.productService.permitted-number-of-calls-in-half-open-state=3
# Automatically transition to HALF_OPEN (don't wait for manual trigger)
resilience4j.circuitbreaker.instances.productService.automatic-transition-from-open-to-half-open-enabled=true
```

### Retry — how it works

Retry wraps the call and automatically re-runs it on failure. Useful for transient failures (brief network blip, momentary overload). Must **NOT** retry on client errors (404, 422) — retrying won't fix "product doesn't exist".

```properties
resilience4j.retry.instances.productService.max-attempts=3
resilience4j.retry.instances.productService.wait-duration=500ms
# Only retry on actual connectivity problems
resilience4j.retry.instances.productService.retry-exceptions=java.io.IOException,feign.RetryableException
# Do NOT retry 404 or unavailable — they're business errors, not transient
resilience4j.retry.instances.productService.ignore-exceptions=feign.FeignException$NotFound,com.example.order_service.exception.ProductNotAvailableException
```

### Annotation execution order

Both annotations on the same method — order matters:

```
CircuitBreaker (outer) → Retry (inner) → actual Feign call
```

- CircuitBreaker checks first: is circuit OPEN? → if yes, skip everything, run fallback
- Retry wraps the Feign call: retries up to 3x before giving up
- If all retries fail → CircuitBreaker records a failure
- After 5+ calls with 50%+ failure rate → circuit OPENS

### Fallback method rules

```java
@CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
@Retry(name = "productService")
public Order createOrder(CreateOrderRequest request) { ... }

// Fallback MUST follow these exact rules:
// 1. Same name as fallbackMethod= value
// 2. Same parameter list as original + Throwable as LAST param
// 3. Same return type as original
public Order createOrderFallback(CreateOrderRequest request, Throwable throwable) {
    // Re-throw business exceptions — don't swallow them
    if (throwable instanceof FeignException.NotFound) throw (FeignException.NotFound) throwable;
    if (throwable instanceof ProductNotAvailableException) throw (ProductNotAvailableException) throwable;

    // For everything else (service down, circuit open): return REJECTED order
    return Order.builder()
        .status(OrderStatus.REJECTED)
        .productName("UNKNOWN - product-service unavailable")
        ...
        .build();
}
```

### What the fallback returns — design decision

| Scenario | Fallback action | Why |
|----------|----------------|-----|
| product-service 404 | Re-throw FeignException.NotFound | Product truly doesn't exist — client should know |
| product unavailable (422) | Re-throw ProductNotAvailableException | Business rule violation — client should know |
| product-service down/timeout | Return REJECTED order | Service issue, not client issue — order saved as REJECTED |
| Circuit OPEN | Same as above | Circuit open means too many failures — same result |

### Actuator endpoints for Resilience4j

```bash
# Circuit breaker state in health
GET /actuator/health       → components.circuitBreakers.status

# All Resilience4j metrics
GET /actuator/metrics      → filter for "resilience4j.*"

# Circuit breaker state specific
GET /actuator/metrics/resilience4j.circuitbreaker.state
GET /actuator/metrics/resilience4j.circuitbreaker.calls

# Retry metrics
GET /actuator/metrics/resilience4j.retry.calls

# Event log (last N events per circuit breaker)
GET /actuator/circuitbreakerevents
GET /actuator/circuitbreakerevents/productService
```

### What was proven in testing

```
✅ GET  /api/orders              → 200 (list orders)
✅ POST /api/orders (valid)      → 201 CONFIRMED (product-service up + circuit CLOSED)
✅ POST /api/orders (no product) → 404 (FeignException.NotFound passes through fallback)
✅ POST /api/orders (unavail)    → 422 (ProductNotAvailableException passes through fallback)
✅ Metrics: resilience4j.circuitbreaker.state shows "closed"
✅ Events: /actuator/circuitbreakerevents records SUCCESS/FAILURE events
✅ Retry metrics: resilience4j.retry.calls shows call counts by kind
```

---

## Phase 8 — Assignment 9: HandlerInterceptor + AOP Performance Logging

**Goal:** Measure how long every API request and every service method takes — without touching any controller or service code. Cross-cutting concerns added through infrastructure (not business logic).

**New files created (in both `product-service` and `order-service`):**
- `interceptor/PerformanceInterceptor.java` — HandlerInterceptor (HTTP-level timing)
- `config/WebMvcConfig.java` — registers the interceptor
- `aspect/ServiceTimingAspect.java` — AOP @Around advice (service-method-level timing)

**New files in `order-service` only:**
- `config/RestTemplateConfig.java` — `@LoadBalanced RestTemplate` bean
- `client/ProductRestTemplateClient.java` — RestTemplate alternative to Feign

### HandlerInterceptor — how it works

```
HTTP Request
    │
    ▼
DispatcherServlet
    │
    ├── preHandle()       ← BEFORE controller → store startTime in request attribute
    │
    ▼
Controller method runs
    │
    ├── postHandle()      ← AFTER controller, BEFORE view → not used for REST
    │
    ▼
Response committed
    │
    └── afterCompletion() ← AFTER everything → calculate + log duration
```

```java
// preHandle — store start time
request.setAttribute("requestStartTime", System.currentTimeMillis());
return true; // must return true or the request is aborted

// afterCompletion — log duration
long duration = System.currentTimeMillis() - (Long) request.getAttribute("requestStartTime");
log.info("[PERF] {} {} → {} | {}ms", method, uri, status, duration);
```

**Why store startTime in request attribute (not ThreadLocal)?**
Virtual threads can migrate between OS threads. Request attribute is tied to the HTTP request object — always safe regardless of threading model.

**Why `addPathPatterns("/api/**")`?**
Without it, every Swagger UI load, every `/actuator/*` ping, every static file — all logged. Noise. Restrict to only your API paths.

### Spring AOP — how it works

```
Client calls orderService.createOrder(request)
    │
    ▼
Spring creates a proxy around OrderService
    │
    ▼
Proxy intercepts the call
    │
    ├── @Around advice runs
    │     ├── record startTime
    │     ├── pjp.proceed()   ← calls the REAL createOrder()
    │     └── record duration, log it
    │
    ▼
Returns result to caller
```

Key vocabulary:

| Term | Meaning |
|------|---------|
| **Aspect** | The class with cross-cutting logic (`ServiceTimingAspect`) |
| **Join Point** | A point where the aspect can run (method execution, field access, etc.) |
| **Pointcut** | Expression that selects which join points to intercept |
| **Advice** | The code that runs — `@Before`, `@After`, `@Around` |
| **Weaving** | Spring wraps your bean in a proxy at startup — you never see it |

```java
// Pointcut — matches ALL methods in ALL classes in the service package
@Pointcut("execution(* com.example.order_service.service.*.*(..))")
public void serviceLayer() {}

// Around advice — wraps each matched method
@Around("serviceLayer()")
public Object measure(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();
    try {
        Object result = pjp.proceed(); // call the real method
        log.debug("[AOP] {}.{}() → {}ms", className, method, elapsed());
        return result;
    } catch (Throwable ex) {
        log.warn("[AOP] {}.{}() threw {} after {}ms", ...);
        throw ex; // NEVER swallow exceptions in an aspect
    }
}
```

**What you see in logs (two layers of timing):**
```
[AOP]  OrderService.createOrder() completed in 45ms    ← service method alone
[PERF] POST /api/orders → 201 | 52ms                  ← full HTTP round-trip
```
The difference (7ms) is Spring MVC overhead (deserialization, validation, serialization).

### @LoadBalanced RestTemplate

```java
@Bean
@LoadBalanced  // This annotation makes Spring intercept all RestTemplate calls
public RestTemplate restTemplate() { return new RestTemplate(); }

// In ProductRestTemplateClient:
restTemplate.getForObject("http://product-service/api/products/{id}", ProductDto.class, id)
//                         ^^^^^^^^^^^^^^^^
//                         Eureka resolves this service name to host:port
```

**Feign vs RestTemplate — when to use each:**

| | Feign | RestTemplate |
|---|---|---|
| Code style | Declarative interface | Imperative method calls |
| Boilerplate | Near-zero | Manual URL building, error handling |
| Best for | Service-to-service calls | Dynamic URLs, streaming, fine control |
| Load balancing | `@FeignClient(name="...")` | `@LoadBalanced` on the bean |
| Circuit breaker | Built-in via Resilience4j | Manual |

---

## Phase 9 — Eureka Server + Service Discovery + Load Balancer

**What Eureka does:**
Every service registers itself with Eureka on startup (name, host, port, health status).
Other services don't need hardcoded URLs — they ask Eureka: "where is `product-service`?"
Eureka returns all healthy instances → LoadBalancer picks one → request goes there.

### Service registry flow

```
1. product-service starts
   → POST http://localhost:8761/eureka/apps/PRODUCT-SERVICE
   → registers: {host: localhost, port: 8082, status: UP}

2. order-service wants to call product-service
   → Feign: @FeignClient(name = "product-service")  ← NO url= anymore
   → Spring Cloud LoadBalancer: "what are the instances of product-service?"
   → Eureka: [{host:localhost, port:8082, status:UP}]
   → LoadBalancer picks one → Feign calls localhost:8082/api/products/{id}

3. If product-service goes DOWN
   → Eureka deregisters it after heartbeat timeout (90s default)
   → LoadBalancer no longer routes to it
   → Circuit breaker opens if failure rate exceeds threshold
```

### Key configuration

```properties
# Every service (except eureka-server itself) needs this:
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true   # register myself
eureka.client.fetch-registry=true          # get the full registry

# eureka-server MUST have both false — it IS the registry, not a client
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### Feign before vs after Eureka

```java
// BEFORE — hardcoded URL, no load balancing
@FeignClient(name = "product-service", url = "${product.service.url}")
// product.service.url=http://localhost:8082

// AFTER — Eureka-based discovery, automatic load balancing
@FeignClient(name = "product-service")
// Spring Cloud LoadBalancer resolves "product-service" via Eureka registry
```

**Startup order (required):**
```
1. eureka-server   (port 8761) → must be first
2. config-server   (port 8888) → needs Eureka, serves config to others
3. product-service (port 8082) → registers with Eureka, pulls config
4. order-service   (port 8083) → same
5. api-gateway     (port 8080) → last, needs all services to route to
```

---

## Phase 9 (cont.) — Assignment 10: Spring Cloud Config Server

**Goal:** Centralise all microservice configuration in one place. Change a property once — all services pick it up.

**New service:** `config-server` (port 8888)

### How it works

```
config-server has these files in src/main/resources/config/:
  application.properties          ← shared by ALL services
  product-service.properties      ← only for product-service
  order-service.properties        ← only for order-service
  api-gateway.properties          ← only for api-gateway

On startup, product-service calls:
  GET http://localhost:8888/product-service/default
  ← returns merged config: product-service.properties + application.properties

Property resolution priority (highest → lowest):
  1. Service-specific file (product-service.properties)
  2. Shared file (application.properties)
  3. Local application.properties in the service itself
```

### Config client setup

```properties
# In every service's application.properties:
# "optional:" = don't fail startup if config-server is unreachable
spring.config.import=optional:configserver:http://localhost:8888
```

### Native vs Git backend

| | Native | Git |
|---|---|---|
| Config location | Classpath / filesystem | Git repository |
| Versioning | None | Full Git history |
| Rollback | Redeploy config-server | `git revert` |
| Use case | Local dev, learning | Production |

```yaml
# config-server application.yml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
```

### Verify config is being served

```bash
# See what config config-server serves for product-service
GET http://localhost:8888/product-service/default
# Returns propertySources[] — merged list of all matching config files
```

---

## Phase 9 (cont.) — Assignment 11: API Gateway (Spring Cloud Gateway)

**Goal:** Single entry point for all clients. Clients call port 8080 — gateway routes to the right service.

**New service:** `api-gateway` (port 8080)

**IMPORTANT:** Spring Cloud Gateway uses **WebFlux (reactive)**, NOT Spring MVC.
- Do NOT add `spring-boot-starter-web` to its pom.xml — conflict!
- Uses Netty (not Tomcat) as embedded server
- Filter methods return `Mono<Void>`, not `void`

### Route configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service-route
          uri: lb://product-service    # lb:// = load-balanced via Eureka
          predicates:
            - Path=/api/products/**    # route when path matches this
          filters:
            - AddResponseHeader=X-Gateway-Routed-By, api-gateway
            - name: Retry
              args:
                retries: 2
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET

        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
```

### GlobalFilter — cross-cutting logging

```java
// Applies to ALL routes automatically (no per-route config needed)
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Log incoming request
        log.info("[GATEWAY] --> {} {}", method, uri);
        long start = System.currentTimeMillis();

        // chain.filter(exchange) = proceed to the next filter/route
        // .then(...) = runs AFTER the entire downstream chain completes
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - start;
            log.info("[GATEWAY] <-- {} | {}ms", status, duration);
        }));
    }

    @Override
    public int getOrder() { return -1; } // run early
}
```

### What was proven in testing

```
✅ POST http://localhost:8080/api/products
   → Gateway routes to product-service:8082 (Eureka resolved)
   → Product created: id=1

✅ POST http://localhost:8080/api/orders
   → Gateway routes to order-service:8083
   → order-service calls product-service via Feign + LoadBalancer
   → Full chain works end-to-end

✅ Eureka registry shows: API-GATEWAY, ORDER-SERVICE, CONFIG-SERVER, PRODUCT-SERVICE

✅ Config-server serves product-service config:
   classpath:/config/product-service.properties
   classpath:/config/application.properties (shared)
```

### Complete service map

```
Client (browser/Postman)
    │ port 8080
    ▼
api-gateway
    ├── /api/products/** → lb://product-service → Eureka → localhost:8082
    └── /api/orders/**   → lb://order-service   → Eureka → localhost:8083
                                                                │
                                         order-service calls product-service
                                         via Feign + lb://product-service
                                         (same Eureka lookup)

Config Server (port 8888) ← all services pull config from here at startup
Eureka Server  (port 8761) ← all services register + discover each other here
```

---

---

## Phase 10 — Docker Compose (Assignment 12)

### What was built

Containerised all 6 services + Kafka using Docker Compose so the entire system can start with a single command.

**Files created:**
- `Dockerfile` in every service — multi-stage build (Maven build → JRE run)
- `docker-compose.yml` at the project root — orchestrates all services

### Multi-stage Dockerfile pattern

Used for all services to avoid needing a pre-build step:

```dockerfile
# Stage 1: Maven builds the fat JAR inside Docker
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B      # Layer cached — only re-runs if pom.xml changes
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Only the JRE + JAR — no Maven, no source code
FROM eclipse-temurin:17-jre-jammy
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why multi-stage?**
- Final image contains only JRE + JAR (~250 MB vs ~700 MB with Maven/JDK)
- `docker-compose up --build` works from scratch — no `mvn package` needed on host

### Docker Compose structure

```
docker-compose.yml
├── kafka           — Bitnami Kafka 3.7 (KRaft mode — no Zookeeper)
├── eureka-server   — port 8761  (depends_on: nothing)
├── config-server   — port 8888  (depends_on: eureka healthy)
├── product-service — port 8082  (depends_on: eureka + config healthy)
├── order-service   — port 8083  (depends_on: eureka + config + product + kafka healthy)
├── payment-service — port 8084  (depends_on: eureka + config + kafka healthy)
└── api-gateway     — port 8080  (depends_on: eureka + config + product + order healthy)
```

### Why `depends_on` with `condition: service_healthy`?

`depends_on: [eureka-server]` only waits for the container to START, not for Spring Boot inside it to be ready.
`condition: service_healthy` waits for the healthcheck to PASS (i.e. `/actuator/health` returns 200).
Without this, services would fail to register with Eureka because Eureka isn't ready yet.

### Environment variable overrides

Spring Boot supports overriding any property via environment variables using relaxed binding:
`eureka.client.service-url.defaultZone` → `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`

| Env Var | Docker value | What it overrides |
|---|---|---|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka-server:8761/eureka/` | `localhost:8761` → Docker service name |
| `SPRING_CONFIG_IMPORT` | `optional:configserver:http://config-server:8888` | `localhost:8888` → Docker service name |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | `localhost:9092` → Docker service name |

### Kafka setup (KRaft mode — no Zookeeper)

Older Kafka (< 3.3) needed Zookeeper as a separate process for cluster metadata.
Kafka 3.3+ ships with **KRaft** — the broker manages its own metadata, no Zookeeper needed.

```yaml
kafka:
  image: bitnami/kafka:3.7
  environment:
    KAFKA_CFG_PROCESS_ROLES: "controller,broker"   # single node plays both roles
    KAFKA_CFG_NODE_ID: "0"
    KAFKA_CFG_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094"
    KAFKA_CFG_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092,EXTERNAL://localhost:9094"
```

Two listeners:
- `PLAINTEXT://kafka:9092` — for Docker-internal service-to-service traffic
- `EXTERNAL://localhost:9094` — for host machine tools (Kafka UI, kafkacat)

### How to run

```bash
# Build all images and start everything
docker-compose up --build

# Start in background
docker-compose up --build -d

# View logs of a specific service
docker-compose logs -f order-service

# Stop and remove containers + volumes
docker-compose down -v
```

### Port map

| Service | Port | URL |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080 |
| eureka-server | 8761 | http://localhost:8761 |
| config-server | 8888 | http://localhost:8888 |
| product-service | 8082 | http://localhost:8082 |
| order-service | 8083 | http://localhost:8083 |
| payment-service | 8084 | http://localhost:8084 |
| kafka (internal) | 9092 | kafka:9092 |
| kafka (external) | 9094 | localhost:9094 |

---

## Phase 6 — Kafka Async Messaging (Assignment 7)

### What was built

Async payment processing using Apache Kafka.

**New service:** `payment-service` (port 8084) — consumes `OrderPlacedEvent` from Kafka.
**Modified:** `order-service` — publishes `OrderPlacedEvent` to Kafka after every confirmed order.

### Why Kafka (async) instead of Feign (sync)?

| | Sync (Feign) | Async (Kafka) |
|---|---|---|
| **order-service waits?** | Yes — blocked until payment responds | No — returns immediately after publish |
| **payment-service down?** | Order creation FAILS | Event is retained in Kafka; processed when it comes back up |
| **Coupling** | Tight — order-service knows about payment-service | Loose — services don't know each other |
| **Add new consumer?** | Must modify order-service | Zero changes — just subscribe to the topic |
| **Ordering** | N/A | Guaranteed within a partition (we key by orderId) |

### Flow

```
POST /api/orders
       │
       ▼
order-service
  ├── 1. Calls product-service (Feign) to validate product
  ├── 2. Creates CONFIRMED order in memory
  ├── 3. Publishes OrderPlacedEvent to topic "orders.placed"  ← NEW
  └── 4. Returns 201 Created immediately

Kafka topic "orders.placed"
       │
       ▼
payment-service (async — independent of the HTTP request)
  └── Consumes event → logs payment processing → APPROVED ✔
```

### Topic design

**Topic name:** `orders.placed`
**Message key:** `orderId` (as String)
**Message value:** `OrderPlacedEvent` (JSON, serialised by `JsonSerializer`)

**Why key = orderId?**
Kafka routes all messages with the same key to the same partition, guaranteeing that events for the same order are processed in order.

### OrderPlacedEvent

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private String customerName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double totalPrice;
}
```

Exists in both services (`order_service.event` and `payment_service.event`).
They are independent copies — no shared library — which is the correct microservices pattern.

### Kafka producer (order-service)

```properties
# application.properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
# Don't add __TypeId__ header — consumer uses its own class name
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

```java
// OrderService.java — inject KafkaTemplate
private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

// After saving order:
kafkaTemplate.send("orders.placed", String.valueOf(order.getId()), event)
    .whenComplete((result, ex) -> {
        if (ex != null) log.error("[KAFKA] Failed: {}", ex.getMessage());
        else log.info("[KAFKA] Published → partition={}, offset={}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    });
```

**`.whenComplete()`** — non-blocking callback: order-service does NOT wait for Kafka acknowledgement before returning the HTTP response.

### Kafka consumer (payment-service)

```properties
# application.properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=payment-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
# Ignore type header from producer — use the class configured below
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.payment_service.event.OrderPlacedEvent
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

```java
@Service
public class PaymentConsumerService {

    @KafkaListener(topics = "orders.placed", groupId = "payment-service-group")
    public void processPayment(OrderPlacedEvent event) {
        log.info("[PAYMENT] Received order: id={}, total=₹{}", event.getOrderId(), event.getTotalPrice());
        // simulate payment gateway call...
        log.info("[PAYMENT] Payment APPROVED ✔  orderId={}", event.getOrderId());
    }
}
```

### Consumer group explained

`groupId = "payment-service-group"` means:
- All replicas of payment-service share this group ID
- Kafka ensures each message is consumed by exactly **one** instance in the group
- If you scale to 3 payment-service instances and the topic has 3 partitions → each instance gets 1 partition → perfect parallelism

### `spring.json.use.type.headers=false` — why?

By default, Spring Kafka's `JsonSerializer` adds a `__TypeId__` header to every message with the fully-qualified class name (e.g. `com.example.order_service.event.OrderPlacedEvent`). The consumer would try to deserialise into that exact class, failing because the class doesn't exist in payment-service's classpath.

Setting `spring.json.add.type.headers=false` on the producer and `spring.json.use.type.headers=false` on the consumer solves this — the consumer just uses the configured default type.

---

## All Assignments Complete ✅

| # | Assignment | Phase | Status |
|---|---|---|---|
| 1–6 | REST API, CI/CD, Feign, Resilience4j, Logging, Actuator | 1–5 | ✅ Done |
| 9 | HandlerInterceptor + AOP | Phase 8 | ✅ Done |
| 10 | Spring Cloud Config Server | Phase 9 | ✅ Done |
| 11 | Eureka + API Gateway + LoadBalancer | Phase 9 | ✅ Done |
| 7 | Kafka async messaging (order → payment) | Phase 6 | ✅ Done |
| 12 | Docker Compose (all services containerised) | Phase 10 | ✅ Done |

---

---

## CI/CD Deep Dive — Jenkins Pipeline in Detail

### What is CI/CD?

**CI — Continuous Integration**
Every time you push code, it is automatically built and tested.
Goal: catch broken code immediately, not when it reaches production.

**CD — Continuous Delivery / Deployment**
After a successful build, the artifact is automatically deployed to an environment.
- **Continuous Delivery** — deploys to staging automatically; production needs a manual approval.
- **Continuous Deployment** — deploys all the way to production automatically.

This project uses **Continuous Delivery**: auto-deploy to TEST and STAGING, manual promotion to PRODUCTION.

---

### The Jenkins Setup

Jenkins runs inside Docker:
```bash
docker run -d \
  --name jenkins \
  -p 8090:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \   # share Docker socket for DinD
  -v jenkins_home:/var/jenkins_home \              # persist Jenkins data
  jenkins/jenkins:lts
```

**Why mount `/var/run/docker.sock`?**
Jenkins needs to run `docker` commands (to build images, run containers) from inside its own container.
Mounting the Docker socket gives the Jenkins container access to the **host's Docker daemon**.
This is called **Docker-in-Docker (DinD)**.

> ⚠️ Security note: Mounting the socket gives the container root-equivalent power on the host.
> Acceptable for dev/CI environments. Use Docker TLS or Kaniko in production.

---

### Git Branching Strategy (Gitflow)

```
main ──────────────────────────────────────────────────── production
  │                  ↑ merge via PR                         deploys here
  │
release/v1 ─────────                                       staging
  │           ↑ cut from main, hotfixes applied here        deploys here
  │
develop ──────────────────────────────────────────────────  test/dev
  │   ↑ features merged here via PR                         deploys here
  │
feature/add-product-api ───────────────── short-lived branch
feature/add-order-service ─────────────── merged to develop when done
```

**Branch → Environment mapping:**
| Branch | Deploys to | Auto or Manual? |
|---|---|---|
| `develop` | TEST (`localhost:8091`) | Auto on push |
| `release/*` | STAGING (`localhost:8092`) | Auto on push |
| `main` | PRODUCTION (`localhost:8093`) | Auto on push (after PR merge) |
| `feature/*` | nowhere | Build + test only |

---

### Jenkinsfile — Full Anatomy

```groovy
pipeline {
    // ── Where to run ────────────────────────────────────────────────────────
    agent any
    // "any" = run on any available Jenkins node/agent.
    // In production: agent { label 'docker-enabled' } to target specific nodes.

    // ── Pipeline-wide variables ──────────────────────────────────────────────
    environment {
        IMAGE_NAME = "eureka-server"
        IMAGE_TAG  = "latest"
        // Use ${env.BUILD_NUMBER} for unique per-build tags in real projects
        // e.g. IMAGE_TAG = "${env.GIT_COMMIT[0..6]}"
    }

    stages {

        // ── Stage 1: Checkout ────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                // scm = Source Control Management — automatically uses the
                // GitHub URL + branch configured in the Jenkins job.
                // After this step: all source files are in the workspace.
            }
        }

        // ── Stage 2: Build ───────────────────────────────────────────────────
        stage('Build') {
            steps {
                sh '''
                    # Stream source INTO Maven container, build, stream target/ back out.
                    # This solves the Docker-in-Docker volume mount problem.
                    tar -czf - . | docker run --rm -i maven:3.9.9-eclipse-temurin-17 sh -c \
                      "tar -xzf - 1>&2 && mvn clean package -DskipTests 1>&2 && tar -czf - target/" \
                      | tar -xzf - -C ./
                '''
                // Why 1>&2 on mvn?
                // Maven writes INFO/ERROR to stdout. Those lines corrupt the
                // tar byte stream being piped back. Redirecting Maven to stderr
                // keeps the pipe clean for ONLY the tar data.
            }
        }

        // ── Stage 3: Build Docker Image ──────────────────────────────────────
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
                // The Dockerfile must be in the current directory (repo root or service dir).
            }
        }

        // ── Stage 4a: Deploy to TEST ─────────────────────────────────────────
        stage('Deploy to TEST') {
            when {
                branch 'develop'
                // This stage ONLY runs when the pipeline is triggered from 'develop' branch.
            }
            steps {
                sh '''
                    docker stop ${IMAGE_NAME}-test 2>/dev/null || true   # stop if running
                    docker rm   ${IMAGE_NAME}-test 2>/dev/null || true   # remove old container
                    docker run -d --name ${IMAGE_NAME}-test \
                               -p 8091:8761 \
                               ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        // ── Stage 4b: Deploy to STAGING ──────────────────────────────────────
        stage('Deploy to STAGING') {
            when {
                branch 'release/*'
                // Matches any branch named release/v1, release/v2, etc.
            }
            steps {
                sh '''
                    docker stop ${IMAGE_NAME}-stg 2>/dev/null || true
                    docker rm   ${IMAGE_NAME}-stg 2>/dev/null || true
                    docker run -d --name ${IMAGE_NAME}-stg \
                               -p 8092:8761 \
                               ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        // ── Stage 4c: Deploy to PRODUCTION ───────────────────────────────────
        stage('Deploy to PRODUCTION') {
            when {
                branch 'main'
                // Only runs when code lands on main (typically via PR merge)
            }
            steps {
                sh '''
                    docker stop ${IMAGE_NAME}-prod 2>/dev/null || true
                    docker rm   ${IMAGE_NAME}-prod 2>/dev/null || true
                    docker run -d --name ${IMAGE_NAME}-prod \
                               -p 8093:8761 \
                               ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }
    }

    // ── Post-build actions ───────────────────────────────────────────────────
    post {
        success {
            echo 'Pipeline succeeded!'
            // In real projects: send Slack/email notification, tag Git commit
        }
        failure {
            echo 'Pipeline FAILED!'
            // In real projects: send alert, rollback to previous image
        }
        always {
            echo 'Cleaning up workspace...'
            // deleteDir() — clean workspace to free disk space
        }
    }
}
```

---

### Pipeline Stage Flow

```
GitHub push
    │
    ▼
Jenkins detects via Webhook (GitHub → Jenkins URL/github-webhook/)
    │
    ▼
┌─────────────┐    ┌────────────┐    ┌──────────────────┐    ┌───────────────┐
│  Checkout   │ →  │   Build    │ →  │ Build Docker Img │ →  │    Deploy     │
│  (git pull) │    │ (mvn pkg)  │    │ (docker build)   │    │ (based on     │
│             │    │            │    │                  │    │  branch name) │
└─────────────┘    └────────────┘    └──────────────────┘    └───────────────┘
      ↑                  ↑                   ↑                       ↑
  always runs        always runs         always runs          conditional on branch
```

---

### Docker-in-Docker — The Volume Mount Problem Explained

**The problem:**

```
Host OS
└── Docker daemon
    └── Jenkins container (running pipeline)
        └── runs: docker run -v $(pwd):/workspace maven ...
                                ↑
                         THIS PATH is evaluated by the HOST Docker daemon.
                         $(pwd) inside Jenkins = /var/jenkins_home/workspace/job
                         But does that path exist ON THE HOST? No!
                         The Jenkins container filesystem ≠ host filesystem.
                         Result: the Maven container starts but /workspace is EMPTY.
```

**Why it fails:**
When Jenkins does `docker run -v /path:/workspace`, the Docker daemon on the **host** mounts `/path` from the **host filesystem**. Jenkins's internal path doesn't exist on the host, so the volume is empty.

**The solution — tar streaming:**
Instead of mounting a volume, stream files through stdin/stdout:

```
Jenkins workspace → gzip → stdin → Maven container
                                        ├── untar source
                                        ├── mvn clean package  (stdout → stderr, keeps pipe clean)
                                        └── tar target/ → stdout
                                                    ↓
                              stdout pipe → Jenkins workspace (untar)
```

```bash
tar -czf - .  \
  | docker run --rm -i maven:3.9.9-eclipse-temurin-17 sh -c \
    "tar -xzf - 1>&2 && mvn clean package -DskipTests 1>&2 && tar -czf - target/" \
  | tar -xzf - -C ./
```

No volume mounts. No host path problems. Works perfectly in any DinD setup.

---

### GitHub Webhook Setup

To trigger Jenkins automatically on every push:

1. **Jenkins side:** Install the "GitHub plugin" → in job config → check "GitHub hook trigger for GITScm polling"
2. **GitHub side:** Repository → Settings → Webhooks → Add webhook
   - Payload URL: `http://<your-jenkins-host>:8090/github-webhook/`
   - Content type: `application/json`
   - Events: "Just the push event"

**Flow:**
```
git push → GitHub → HTTP POST to Jenkins → Jenkins starts pipeline
```

---

### Environment Variables in Jenkins

```groovy
environment {
    // Static values
    IMAGE_NAME = "my-service"

    // Dynamic values from Jenkins built-ins
    BUILD_NUMBER = "${env.BUILD_NUMBER}"      // auto-increment: 1, 2, 3...
    GIT_BRANCH   = "${env.BRANCH_NAME}"       // develop / main / release/v1
    GIT_COMMIT   = "${env.GIT_COMMIT}"        // full SHA: abc123def456...
    SHORT_SHA    = "${env.GIT_COMMIT[0..6]}"  // short: abc123d

    // Secrets — stored in Jenkins Credentials (not in code!)
    // DOCKER_PASS = credentials('docker-hub-password')
}
```

**Best practice for image tagging:**
```groovy
IMAGE_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
// Produces: develop-42, main-15, release/v2-3
// Every build gets a unique tag — you can always roll back to any version.
```

---

---

## Complete Application Flow

### System Architecture Map

```
════════════════════════════════════════════════════════════════════════════
                     MICROSERVICE SYSTEM ARCHITECTURE
════════════════════════════════════════════════════════════════════════════

  ┌─────────────────────────────────────────────────────┐
  │               SUPPORT LAYER                         │
  │                                                     │
  │  ┌──────────────────┐   ┌──────────────────────┐   │
  │  │  eureka-server   │   │    config-server     │   │
  │  │     :8761        │   │       :8888          │   │
  │  │                  │   │                      │   │
  │  │  Service         │   │  Serves config       │   │
  │  │  Registry        │   │  files to all        │   │
  │  │                  │   │  services at         │   │
  │  │  All services    │   │  startup             │   │
  │  │  register here   │   │                      │   │
  │  └──────────────────┘   └──────────────────────┘   │
  └─────────────────────────────────────────────────────┘

             ↕ register                ↕ pull config
  ┌──────────────────────────────────────────────────────────────┐
  │                    BUSINESS LAYER                            │
  │                                                              │
  │        ┌─────────────────┐   ┌──────────────────┐           │
  │        │  product-service│   │  order-service   │           │
  │        │     :8082       │   │     :8083        │           │
  │        │                 │◄──┤                  │           │
  │        │  Products CRUD  │   │  Creates orders  │           │
  │        │                 │   │  Calls product-  │           │
  │        │                 │   │  service (Feign) │           │
  │        └─────────────────┘   └──────────────────┘           │
  │                                      │                       │
  │                               publishes event                │
  │                                      ▼                       │
  │  ┌──────────────────────────────────────────────────────┐   │
  │  │              kafka :9092                             │   │
  │  │  topic: "orders.placed"                              │   │
  │  └──────────────────────────────────────────────────────┘   │
  │                              │                               │
  │                       consumes event                         │
  │                              ▼                               │
  │                  ┌──────────────────────┐                   │
  │                  │  payment-service     │                   │
  │                  │     :8084            │                   │
  │                  │  Processes payments  │                   │
  │                  │  asynchronously      │                   │
  │                  └──────────────────────┘                   │
  └──────────────────────────────────────────────────────────────┘

              ↕ all client traffic goes through here
  ┌──────────────────────────────────────────────────────────────┐
  │                      GATEWAY LAYER                           │
  │                                                              │
  │                  ┌─────────────────────┐                    │
  │                  │    api-gateway      │                    │
  │                  │       :8080         │                    │
  │                  │                     │                    │
  │  /api/products/**│  lb://product-service (via Eureka)      │
  │  /api/orders/** →│  lb://order-service   (via Eureka)      │
  │                  └─────────────────────┘                    │
  └──────────────────────────────────────────────────────────────┘
                              ↑
                     ALL CLIENT REQUESTS
                     (Postman / Browser / Frontend)
```

---

### Startup Sequence

Services must start in this order due to dependencies. Docker Compose enforces this via `condition: service_healthy`:

```
Step 1:  kafka          starts (KRaft broker ready)
         eureka-server  starts (no dependencies)

Step 2:  config-server  starts (needs eureka healthy)
                              → registers itself in Eureka

Step 3:  product-service starts (needs eureka + config healthy)
         payment-service starts (needs eureka + config + kafka healthy)
                              → both pull config from config-server
                              → both register in Eureka

Step 4:  order-service  starts (needs eureka + config + product + kafka healthy)
                              → pulls config from config-server
                              → registers in Eureka
                              → Feign client can now resolve "product-service"

Step 5:  api-gateway    starts (needs eureka + config + product + order healthy)
                              → pulls config from config-server
                              → registers in Eureka
                              → loads routes from application.yml
                              → can now route traffic to all services
```

---

### Flow 1 — Creating a Product

```
Client: POST http://localhost:8080/api/products
        Body: {"name":"Laptop","price":75000,"available":true}

Step 1: api-gateway receives request
        ├── RequestLoggingFilter logs: [GATEWAY] --> POST /api/products
        ├── Matches route: Path=/api/products/**
        └── Resolves lb://product-service via Eureka → product-service:8082

Step 2: product-service receives request
        ├── PerformanceInterceptor.preHandle() logs incoming request, records startTime
        ├── MdcFilter injects requestId into MDC (all logs tagged with this ID)
        ├── ProductController.createProduct() called
        ├── @Valid runs Bean Validation on the request body
        │       └── if invalid → GlobalExceptionHandler returns 400 + field errors
        ├── ProductService.createProduct() called (ServiceTimingAspect wraps it)
        │       ├── Assigns auto-incremented id
        │       └── Saves to in-memory productStore HashMap
        ├── Returns ResponseEntity with 201 Created + created product body
        └── PerformanceInterceptor.afterCompletion() logs duration

Step 3: api-gateway receives response
        ├── Adds header: X-Gateway-Routed-By: api-gateway
        ├── RequestLoggingFilter logs: [GATEWAY] <-- 201 | 12ms
        └── Returns 201 to client

Response: {"id":1,"name":"Laptop","price":75000,"available":true}
```

---

### Flow 2 — Creating an Order (Full Chain)

```
Client: POST http://localhost:8080/api/orders
        Body: {"customerName":"Ankur","productId":1,"quantity":2}

Step 1: api-gateway
        └── Routes to lb://order-service → order-service:8083

Step 2: order-service receives request
        ├── PerformanceInterceptor records start time
        ├── OrderController.createOrder() → OrderService.createOrder()
        │
        ├─── RESILIENCE4J WRAP ──────────────────────────────────────────────
        │    @CircuitBreaker(name="productService", fallbackMethod="createOrderFallback")
        │    @Retry(name="productService")
        │    │
        │    ├── ProductClient.getProductById(1) ← Feign client call
        │    │       ├── Feign resolves "product-service" via Eureka → :8082
        │    │       ├── Makes GET http://product-service:8082/api/products/1
        │    │       └── Returns ProductDto{id=1, name="Laptop", price=75000}
        │    │
        │    ├── Check: product.isAvailable() == true ✔
        │    ├── Calculate: totalPrice = 75000 × 2 = 150000
        │    ├── Build Order{id=1, status=CONFIRMED, totalPrice=150000}
        │    └── Save to orderStore
        │
        ├── KAFKA PUBLISH (async, non-blocking) ──────────────────────────────
        │    kafkaTemplate.send("orders.placed", "1", OrderPlacedEvent{...})
        │    .whenComplete((result, ex) → log partition + offset)
        │    ← order-service does NOT wait for this to complete
        │
        └── Returns 201 Created immediately
               {"id":1,"status":"CONFIRMED","totalPrice":150000}

Step 3: (parallel, async) payment-service
        ├── @KafkaListener(topics="orders.placed") fires
        ├── Deserializes JSON → OrderPlacedEvent
        ├── Logs:  [PAYMENT] orderId=1, customer=Ankur, total=₹150000
        ├── Simulates payment gateway call (200ms delay)
        └── Logs:  [PAYMENT] Payment APPROVED ✔ orderId=1

Client gets 201 response in ~50ms.
Payment processing happens ~250ms later, completely independently.
```

---

### Flow 3 — Circuit Breaker in Action

```
Normal state (circuit CLOSED):
  order-service → Feign → product-service  ← works fine

product-service goes down:
  Attempt 1: Feign → connection refused → Retry waits 500ms
  Attempt 2: Feign → connection refused → Retry waits 500ms
  Attempt 3: Feign → connection refused → all retries exhausted
  → CircuitBreaker records 1 failure

After 5+ calls, 50%+ failure rate:
  CircuitBreaker OPENS
  → No more Feign calls made (fast-fail)
  → Fallback runs immediately: returns REJECTED order

After 10 seconds (wait-duration-in-open-state):
  CircuitBreaker → HALF_OPEN
  → Allows 3 test calls through
  If test calls succeed → CLOSED again (recovered)
  If test calls fail   → OPEN again
```

---

### Flow 4 — Config Server Pull at Startup

```
Service starts (e.g. product-service)
    │
    ▼
Spring Boot reads: spring.config.import=optional:configserver:http://config-server:8888
    │
    ▼
Config Client makes GET http://config-server:8888/product-service/default
    │
    ▼
Config Server looks in classpath:/config/ for:
    ├── application.properties          (shared — all services get this)
    └── product-service.properties      (service-specific — only product-service gets this)
    │
    ▼
Returns merged properties as JSON
    │
    ▼
product-service applies them — they override/supplement local application.properties
    │
    ▼
Spring Boot continues startup with combined configuration
```

---

### Flow 5 — Eureka Registration + Discovery

```
Service startup:
  product-service → POST http://eureka-server:8761/eureka/apps/PRODUCT-SERVICE
                    Body: {hostname, ip, port, status=UP, healthCheckUrl, ...}
  Eureka stores this in its in-memory registry.

Heartbeat (every 30 seconds):
  product-service → PUT http://eureka-server:8761/eureka/apps/PRODUCT-SERVICE/<instanceId>
  If Eureka doesn't hear from a service for 90 seconds → removes it from registry.

Service discovery (by order-service's Feign):
  Feign resolves "product-service" →
    asks Spring Cloud LoadBalancer →
      LoadBalancer asks Eureka client (local cache, refreshed every 30s) →
        Returns list of instances: [product-service:8082]
          Picks one (round-robin by default) →
            Makes actual HTTP call to that IP:port
```

---

### Flow 6 — Request through the full Docker Compose stack

```
docker-compose up --build
    │
    ├── Docker builds each service image (multi-stage: Maven → JRE)
    ├── Creates microservices-net bridge network
    └── Starts containers in dependency order (healthcheck-gated)

Client: curl http://localhost:8080/api/orders -d '{...}'
    │
    │  Host machine port 8080
    ▼
Docker forwards → api-gateway container :8080
    │
    │  Docker bridge network (microservices-net)
    │  All containers communicate by service name: kafka, eureka-server, etc.
    ▼
api-gateway → eureka-server:8761 (name resolves to container IP)
    │          Eureka returns: order-service is at order-service:8083
    ▼
api-gateway → order-service:8083
    ▼
order-service → product-service:8082 (Feign + Eureka)
    ▼
order-service → kafka:9092 (publishes event)
    ▼
kafka → payment-service (consumer receives event)
```

---

---

## What to Use When — Decision Guide

### 1. Service-to-Service Communication: Feign vs RestTemplate vs Kafka

#### Use **Feign Client** when:
- You need a **synchronous** response before continuing
- The calling service needs the data to complete its job
- Example: order-service needs product details (price, availability) to calculate total
- Result is returned **immediately** in the same HTTP request

```java
// Simple, declarative — just define the interface
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable Long id);
}
```

#### Use **RestTemplate** (with `@LoadBalanced`) when:
- You want more control over the HTTP request (headers, error handling)
- You're in an older Spring Boot project (RestTemplate was the standard before Feign)
- You need to call an external API that isn't registered in Eureka

```java
// More verbose, but more control
restTemplate.getForObject("http://product-service/api/products/" + id, ProductDto.class);
```

#### Use **Kafka** (async messaging) when:
- The calling service does NOT need to wait for the result
- You want the downstream service to be independently scalable
- You want the system to work even when the downstream service is temporarily down
- Example: order-service publishes an event; payment-service processes it later
- Multiple services might need to react to the same event (fan-out)

```
Rule of thumb:
  "Do I need the answer right now to continue?"
  YES → Feign (sync)
  NO  → Kafka (async)
```

---

### 2. Resilience: Circuit Breaker vs Retry vs Rate Limiter vs Bulkhead

#### Use **@Retry** when:
- The failure is likely **transient** (temporary network hiccup, momentary overload)
- The service will recover within seconds
- Do NOT retry on: 404 (wrong ID), 400 (bad input), 422 (business rule failure) — those won't fix themselves
- Retry on: `IOException`, `TimeoutException`, `500 Internal Server Error`

```java
@Retry(name = "productService")
// Configured: maxAttempts=3, waitDuration=500ms
// Do NOT retry: FeignException.NotFound, ProductNotAvailableException
```

#### Use **@CircuitBreaker** when:
- A downstream service is repeatedly failing (not just occasional hiccups)
- You want to stop hammering a struggling service
- You want fast-fail behaviour instead of waiting for timeouts on every call
- The fallback should return a cached result, a default value, or a REJECTED status

```java
@CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
// Opens after 50% failure rate over 10 calls
// Stays open 10 seconds, then tries again
```

**Retry + CircuitBreaker together (correct order):**
```
Request → CircuitBreaker (outer) → Retry (inner) → Feign call
```
- If circuit OPEN → fallback immediately (no retry attempted)
- If circuit CLOSED → Retry wraps the Feign call → records result for CircuitBreaker

#### Use **Rate Limiter** when:
- You want to limit how many calls YOUR service makes to a downstream in a time window
- Example: you're calling a paid external API with a request quota

#### Use **Bulkhead** when:
- You want to isolate a slow downstream from consuming all your threads
- Example: product-service is slow; without bulkhead, it would exhaust your thread pool and starve all other requests

---

### 3. API Gateway vs Direct Service Calls

#### Use **API Gateway** when:
- Clients (browser, mobile app, other services) should have a **single entry point**
- You want centralized cross-cutting concerns: auth, rate limiting, logging, SSL termination
- You want to hide internal service topology from clients (don't expose :8082, :8083 directly)
- This is the standard production pattern

#### Skip the gateway for:
- Service-to-service calls **inside** the cluster (order-service → product-service via Feign)
- Internal calls should go directly via Eureka to avoid extra network hop + bottleneck

---

### 4. Config Management: Config Server vs Environment Variables vs Local Properties

#### Use **Config Server** when:
- You have multiple services sharing common config (logging levels, actuator settings)
- You want to change config **without restarting** services (with `@RefreshScope`)
- You want different config per environment (application-dev.properties vs application-prod.properties)
- You're running many instances and need config consistency

#### Use **Environment Variables** when:
- The value is **environment-specific** and changes per deployment (DB URL, API keys, service URLs)
- You're running in Docker/Kubernetes (standard practice: 12-Factor App)
- Secret values that should never be in source code

```yaml
# docker-compose.yml
environment:
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
  # localhost → docker service name, different per environment
```

#### Use **Local `application.properties`** for:
- Defaults that work for local development
- Non-sensitive structural config that rarely changes
- The config-server should OVERRIDE these, not replace them entirely

**Priority order (highest → lowest):**
```
Environment Variables  (highest — override everything)
    ↓
Config Server properties
    ↓
Local application.properties  (lowest — default values)
```

---

### 5. Observability: Filter vs Interceptor vs AOP

#### Use **Filter** (`OncePerRequestFilter`) when:
- You need to act on the raw `HttpServletRequest` **before** Spring MVC processes it
- Use case: inject a request ID into MDC so all logs in a request share the same ID
- Runs for ALL requests, including static resources and Actuator endpoints

```java
// MdcFilter — runs first, injects requestId
MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));
```

#### Use **HandlerInterceptor** when:
- You need access to the matched **handler method** (which controller method will handle it)
- Use case: measure HTTP request/response time, log method + URI + status + duration
- Only runs for requests that match a Spring MVC handler (not static resources)
- Can run code both before (`preHandle`) and after (`afterCompletion`) with access to the response

```java
// PerformanceInterceptor
preHandle()      → record start time
afterCompletion() → calculate duration, log [PERF] 12ms
```

#### Use **AOP** (`@Aspect` + `@Around`) when:
- You want to measure/intercept **service layer methods** (not HTTP layer)
- Use case: measure how long `ProductService.createProduct()` takes
- Can target any bean method by annotation or package name
- The method target doesn't need to know it's being measured

```java
// ServiceTimingAspect — wraps all @Service methods
@Around("within(@org.springframework.stereotype.Service *)")
// Logs: [AOP] ProductService.createProduct() completed in 8ms
```

**Quick comparison:**
| Tool | Scope | Use for |
|---|---|---|
| Filter | Servlet level (all requests) | Request ID injection, auth tokens |
| HandlerInterceptor | Spring MVC level (mapped requests) | HTTP timing, auth checks |
| AOP @Around | Any Spring bean | Service timing, transaction logging |

---

### 6. Deployment: Docker Compose vs Raw Docker vs Kubernetes

#### Use **`docker run`** (raw Docker) when:
- Running a single container for quick testing
- CI/CD pipeline builds + starts one service
- Not managing multiple services together

#### Use **Docker Compose** when:
- Running the entire multi-service application locally
- Development and testing environments
- Small production deployments (single server, low traffic)
- Services have startup order dependencies
- `docker-compose up --build` — everything in one command

#### Use **Kubernetes** when:
- Production with high availability requirements (auto-restart, multiple replicas)
- You need auto-scaling based on CPU/memory/custom metrics
- You need rolling deployments (zero-downtime updates)
- Managing dozens of services across multiple machines

```
Development    → docker-compose up
Staging/CI     → docker-compose up (or Kubernetes)
Production     → Kubernetes (EKS, GKE, AKS) or Docker Swarm
```

---

### 7. Logging Levels: When to Use What

| Level | Use when | Example |
|---|---|---|
| `ERROR` | Something broke and needs immediate attention | DB connection failed, NullPointerException |
| `WARN` | Something unexpected but recoverable | Circuit breaker opened, slow request (>500ms), retry attempt |
| `INFO` | Normal business events worth recording | Order created, service started, payment approved |
| `DEBUG` | Detailed internal flow (off in production) | Feign request URL, AOP method timing, cache hit/miss |
| `TRACE` | Extremely verbose (almost never used) | Every line of a method, SQL bind parameters |

**Rule of thumb:**
- `INFO` — what happened (the outcome)
- `DEBUG` — how it happened (the steps)
- `ERROR` — what went wrong (with stack trace)
- `WARN` — could be a problem, worth investigating

---

### 8. HTTP Methods: Which to Use When

| Method | Use when | Status | Body? |
|---|---|---|---|
| `GET` | Fetching data (safe, idempotent) | 200 OK | Response only |
| `POST` | Creating a new resource | **201 Created** | Request + Response |
| `PUT` | Replacing an entire resource | 200 OK | Request + Response |
| `PATCH` | Updating part of a resource | 200 OK | Request + Response |
| `DELETE` | Removing a resource | **204 No Content** | None |

**Idempotent** = calling the same request multiple times produces the same result.
- `GET`, `PUT`, `DELETE` = idempotent ✔
- `POST` = NOT idempotent (creates a new resource each time) ✗

**Safe** = read-only, no side effects.
- Only `GET` is safe.

---

### 9. Spring Annotations Cheat Sheet — What They Do

| Annotation | Where | What it does |
|---|---|---|
| `@SpringBootApplication` | Main class | Combines @Configuration + @EnableAutoConfiguration + @ComponentScan |
| `@RestController` | Class | @Controller + @ResponseBody — returns JSON, not views |
| `@RequestMapping` / `@GetMapping` etc. | Method | Maps HTTP method + URL to this method |
| `@Service` | Class | Business logic layer — also an AOP pointcut target |
| `@Component` | Class | Generic Spring-managed bean |
| `@Configuration` | Class | Contains @Bean factory methods |
| `@Bean` | Method | Registers the return value as a Spring bean |
| `@Autowired` | Field/Constructor | Spring injects the dependency |
| `@Value("${property}")` | Field | Injects a property value |
| `@Valid` | Parameter | Triggers Bean Validation on the annotated object |
| `@PathVariable` | Parameter | Extracts value from URL path: `/products/{id}` |
| `@RequestBody` | Parameter | Deserializes JSON body to Java object |
| `@ExceptionHandler` | Method | Handles a specific exception type |
| `@RestControllerAdvice` | Class | Global exception handler for all controllers |
| `@FeignClient` | Interface | Declares a declarative HTTP client |
| `@LoadBalanced` | Bean method | RestTemplate or WebClient uses Eureka for load balancing |
| `@KafkaListener` | Method | Listens to a Kafka topic |
| `@EnableEurekaServer` | Main class | Turns this app into the Eureka registry |
| `@EnableDiscoveryClient` | Main class | Registers this service with Eureka |
| `@EnableConfigServer` | Main class | Turns this app into the Config Server |
| `@CircuitBreaker` | Method | Wraps method with Resilience4j circuit breaker |
| `@Retry` | Method | Wraps method with Resilience4j retry logic |
| `@Aspect` | Class | Marks this class as an AOP aspect |
| `@Around` | Method | AOP advice that wraps the target method (before + after) |

---
