# 🚀 JPA / Hibernate & AOP — Interview Guide for 5-Year Java Developers

> **Who is this for?** A Java developer with ~5 years of experience who needs to nail the interview.
> Every section goes **Basic → Intermediate → Advanced** with real code, gotchas, and expected interview Q&A.

---

## 📚 Table of Contents

1. [JPA & Hibernate — The Big Picture](#1-jpa--hibernate--the-big-picture)
2. [Entity Mapping](#2-entity-mapping)
3. [Relationships](#3-relationships)
4. [Lazy vs Eager Loading](#4-lazy-vs-eager-loading)
5. [Transactions](#5-transactions)
6. [Query Optimization](#6-query-optimization)
7. [AOP — Aspect-Oriented Programming](#7-aop--aspect-oriented-programming)
8. [Quick-Fire Interview Q&A Cheatsheet](#8-quick-fire-interview-qa-cheatsheet)
9. [🏭 When to Use What — Industry Decision Guide](#9--when-to-use-what--industry-decision-guide)

---

## 1. JPA & Hibernate — The Big Picture

### 🔰 Basic — What Are They?

| Term | What it is |
|------|-----------|
| **JDBC** | Raw SQL calls. You write everything by hand. |
| **JPA** | A **specification** (set of rules/interfaces). It defines HOW an ORM should work. |
| **Hibernate** | A **JPA implementation** — the most popular one. Does the actual heavy lifting. |
| **ORM** | Object-Relational Mapping — maps Java objects ↔ DB tables automatically. |

```
Your Code  →  JPA (interface)  →  Hibernate (implementation)  →  JDBC  →  Database
```

**Simple analogy:**
- JPA is like a USB standard (the spec).
- Hibernate is like a specific USB cable brand (the implementation).
- You code to the **standard** (JPA), swap the brand if needed.

### ⚙️ Intermediate — How Hibernate Works Internally

Hibernate uses a **Session** (or EntityManager in JPA) as a **Unit of Work**:

```
Application → EntityManager → First-Level Cache (Session) → DB
```

Key internal components:

| Component | Purpose |
|-----------|---------|
| `EntityManager` | Main API to interact with the persistence context |
| `Session` | Hibernate-specific EntityManager wrapper |
| `SessionFactory` | Thread-safe, heavyweight — created once per app |
| `PersistenceContext` | In-memory cache (1st level) — lives for one transaction |
| `EntityManagerFactory` | JPA equivalent of SessionFactory |

### 🧠 Advanced — Entity Lifecycle States

Every entity object goes through these states:

```
new/transient ──── persist() ───→ MANAGED (tracked by PC)
                                       │
                                   commit/flush → SQL sent to DB
                                       │
                               detach()/close() → DETACHED
                                       │
                                   merge() → back to MANAGED
                                       │
                                 remove() → REMOVED
```

| State | In DB? | Tracked by Hibernate? |
|-------|--------|-----------------------|
| **Transient** | No | No |
| **Managed** | Yes (or pending insert) | ✅ Yes |
| **Detached** | Yes | ❌ No |
| **Removed** | Will be deleted | ✅ (until commit) |

> 🎯 **Interview Tip:** "What happens when you call `save()` vs `persist()` vs `merge()`?"
> - `persist()` — makes transient → managed; fails if entity already has an ID
> - `merge()` — copies state of detached entity into managed entity; returns managed instance
> - `save()` — Hibernate-specific; similar to persist but returns generated ID immediately

### 🏭 Industry Decision — JPA/Hibernate vs Alternatives

> **"When would you NOT use Hibernate in production?"** — This is a senior-level question.

| Situation | Use This | Why |
|-----------|----------|-----|
| Standard CRUD app (e-commerce, CMS, ERP) | **JPA + Hibernate** | Productivity, dirty checking, caching, relationships |
| Complex reporting with 10+ table JOINs | **Native SQL / JOOQ** | JPQL gets painful, native SQL is cleaner and faster |
| Bulk insert/update of millions of rows | **JDBC / Spring JDBC Template** | Hibernate overhead is too high; JDBC batching is faster |
| Read-heavy microservice (no writes) | **Spring Data JDBC or JOOQ** | No ORM overhead; simpler, predictable SQL |
| Simple key-value CRUD, no relationships | **Spring Data JDBC** | Lighter than Hibernate, no proxy/session overhead |
| Event sourcing / CQRS systems | **Custom / JOOQ** | ORM's identity map doesn't fit event-based models |
| Distributed systems with NoSQL | **Spring Data MongoDB/Redis** | JPA is relational only |

**Real industry pattern:**
```
Microservice A (complex domain, many relationships)  → JPA + Hibernate ✅
Microservice B (read-heavy reporting service)        → JOOQ / Native SQL ✅
Microservice C (bulk ETL data pipeline)              → Spring JDBC Template ✅
Microservice D (product catalog, mostly reads)       → Spring Data JDBC ✅
```

> 🎯 **Interview Answer:** *"We use Hibernate for our core domain services where we have rich object models and complex relationships. For reporting and analytics queries, we drop down to native SQL or JOOQ because the queries are too complex for JPQL and we need fine-grained control over execution plans."*

---

## 2. Entity Mapping

### 🔰 Basic — Annotations You Must Know

```java
@Entity                          // marks this class as a DB table
@Table(name = "orders")          // custom table name
public class Order {

    @Id                          // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(length = 255, unique = true)
    private String trackingNumber;

    @Transient                   // NOT mapped to DB column
    private String tempField;

    @Enumerated(EnumType.STRING) // store enum as VARCHAR, not number
    private OrderStatus status;

    @Lob                         // for large text or binary
    private String description;
}
```

### ⚙️ Intermediate — ID Generation Strategies

| Strategy | How it works | Use When |
|----------|-------------|----------|
| `AUTO` | Hibernate decides | Prototyping only |
| `IDENTITY` | DB auto-increment (MySQL, PG) | Most common for relational DBs |
| `SEQUENCE` | DB sequence object | Oracle, PostgreSQL (better performance) |
| `TABLE` | Separate ID table | Portability across all DBs (slowest) |
| `UUID` | Generate UUID | Distributed systems, no DB roundtrip |

```java
// UUID example
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
@Column(updatable = false, nullable = false)
private UUID id;
```

### 🧠 Advanced — Embeddable Types & Inheritance

**@Embeddable — Value Objects (no own ID):**
```java
@Embeddable
public class Address {
    private String street;
    private String city;
    private String zipCode;
}

@Entity
public class Customer {
    @Id @GeneratedValue
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "billing_street"))
    })
    private Address billingAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street"))
    })
    private Address shippingAddress;
}
```

**Inheritance Strategies:**

```java
// Strategy 1: SINGLE_TABLE — all subclasses in one table (fastest queries, nulls everywhere)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment { ... }

@Entity
@DiscriminatorValue("CREDIT")
public class CreditCardPayment extends Payment { ... }

// Strategy 2: TABLE_PER_CLASS — each subclass has its own full table (no joins, but UNION queries)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Payment { ... }

// Strategy 3: JOINED — parent table + child tables joined (most normalized, slower)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment { ... }
```

| Strategy | Pros | Cons |
|----------|------|------|
| SINGLE_TABLE | Best performance, simple queries | Lots of nullable columns |
| JOINED | Clean schema, normalized | Requires JOINs on every query |
| TABLE_PER_CLASS | No NULLs, each class independent | UNION queries, no polymorphic FK |

> 🎯 **Interview Tip:** "SINGLE_TABLE is the default and usually fastest for reads. Use JOINED when you need a clean schema and the extra JOIN cost is acceptable."

### 🏭 Industry Decision — Entity Mapping Choices

**Which ID strategy in production?**

| Scenario | Strategy | Real reason |
|----------|----------|-------------|
| MySQL/MariaDB monolith | `IDENTITY` | Native AUTO_INCREMENT, simplest setup |
| PostgreSQL app | `SEQUENCE` | Sequences allow Hibernate to batch INSERTs (IDENTITY breaks batching!) |
| Microservices / distributed system | `UUID` | No DB roundtrip needed; IDs safe to generate in app layer; no ID collision across services |
| Oracle enterprise app | `SEQUENCE` | Oracle doesn't support IDENTITY natively in older versions |
| Never use in production | `TABLE` | Separate ID table = bottleneck under load, needs locking |
| Never use blindly | `AUTO` | Unpredictable — let Hibernate pick randomly? No. |

> ⚠️ **Real gotcha in industry:** Using `IDENTITY` with PostgreSQL + Hibernate **disables batch inserts** because Hibernate needs the ID immediately after each INSERT. Switch to `SEQUENCE` and watch bulk insert performance jump 5–10x.

**Which Inheritance strategy in production?**

| Scenario | Strategy |
|----------|----------|
| Payment types (Credit, Debit, UPI) — few subtypes, mostly read by type | `SINGLE_TABLE` — fast, simple |
| Employee hierarchy (Manager, Engineer) — report on all employees at once | `JOINED` — clean schema, polymorphic queries OK |
| Notification types (Email, SMS, Push) — each type queried independently | `TABLE_PER_CLASS` — no JOINs per type, but no polymorphic FK |
| DDD value objects like Address, Money | `@Embeddable` — not inheritance; embed directly |

**When to use `@Embeddable` vs a separate `@Entity`?**
```
Use @Embeddable when:
  ✅ The object has no identity of its own (no ID, no lifecycle)
  ✅ It always lives and dies with its parent (Address, Money, DateRange)
  ✅ It's never referenced from another entity

Use @Entity when:
  ✅ It has its own ID and can exist independently
  ✅ Multiple entities can reference it (Product referenced by OrderItem and CartItem)
  ✅ It has its own audit trail / created_at, updated_at
```

---

## 3. Relationships

### 🔰 Basic — The Four Types

| Annotation | Meaning | Example |
|------------|---------|---------|
| `@OneToOne` | One row ↔ One row | User ↔ UserProfile |
| `@OneToMany` | One row ↔ Many rows | Order → OrderItems |
| `@ManyToOne` | Many rows → One row | OrderItem → Order |
| `@ManyToMany` | Many ↔ Many | Student ↔ Course |

### ⚙️ Intermediate — Owning Side & mappedBy

> ⚠️ **This trips up most candidates!**

In a bidirectional relationship, ONE side **owns** the relationship (holds the FK), the other side uses `mappedBy`.

```java
// ORDER is the "parent"
@Entity
public class Order {

    @Id @GeneratedValue
    private Long id;

    // "items" field on OrderItem is the OWNER (has the FK column)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}

// ORDER_ITEM is the "owner" — it has the foreign key column
@Entity
public class OrderItem {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")   // this column exists in ORDER_ITEM table
    private Order order;
}
```

**Rule of thumb:**
- The `@ManyToOne` / `@JoinColumn` side = **owner** (controls FK)
- The `@OneToMany(mappedBy = "...")` side = **inverse** (read-only mirror)

**Always use helper methods to keep both sides in sync:**
```java
public class Order {
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);  // keep both sides in sync!
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
```

### ⚙️ Intermediate — ManyToMany

```java
@Entity
public class Student {

    @Id @GeneratedValue
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(
        name = "student_course",           // junction table name
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany(mappedBy = "courses")      // inverse side
    private Set<Student> students = new HashSet<>();
}
```

> ⚠️ **Gotcha:** If you need extra columns in the junction table (e.g., `enrolled_date`), use a dedicated `@Entity` for the join table instead of `@ManyToMany`.

```java
@Entity
@Table(name = "student_course")
public class StudentCourse {

    @EmbeddedId
    private StudentCourseId id;

    @ManyToOne @MapsId("studentId")
    private Student student;

    @ManyToOne @MapsId("courseId")
    private Course course;

    private LocalDate enrolledDate;  // extra column
}

@Embeddable
public class StudentCourseId implements Serializable {
    private Long studentId;
    private Long courseId;
}
```

### 🧠 Advanced — Cascade Types & orphanRemoval

| Cascade Type | What it does |
|-------------|-------------|
| `PERSIST` | Save child when parent is saved |
| `MERGE` | Merge child when parent is merged |
| `REMOVE` | Delete child when parent is deleted |
| `REFRESH` | Refresh child when parent is refreshed |
| `DETACH` | Detach child when parent is detached |
| `ALL` | All of the above |

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items;
```

- `cascade = ALL` → any operation on Order is cascaded to its Items
- `orphanRemoval = true` → if an item is removed from the collection, it's deleted from DB

> 🎯 **Interview Tip:** `orphanRemoval = true` is like adding `cascade = REMOVE` specifically for collection removal, but only works on `@OneToMany` and `@OneToOne`. Use it when child entities have no meaning without the parent.

### 🏭 Industry Decision — Relationships

**Which relationship to use when?**

| Real-world case | Use | Notes |
|----------------|-----|-------|
| User ↔ UserProfile (one account, one profile) | `@OneToOne` | Put FK on the dependent side (UserProfile has user_id) |
| Order → OrderItems (one order, many line items) | `@OneToMany` / `@ManyToOne` | Always bidirectional; use `orphanRemoval = true` |
| Product ↔ Category (a product has one category) | `@ManyToOne` on Product | Unidirectional is fine here |
| Student ↔ Course (many students, many courses) | `@ManyToMany` (simple) or junction entity | Use junction entity when you need enrollment date, grade, etc. |
| Employee → Manager (self-referencing) | `@ManyToOne` self-join | `@JoinColumn(name = "manager_id")` pointing to same table |

**Which Cascade to use in production?**

| Scenario | Cascade | Why |
|----------|---------|-----|
| Order + OrderItems (items have no meaning alone) | `ALL` + `orphanRemoval = true` | Delete order → delete all items automatically |
| User + Address (address shared? No, user-specific) | `PERSIST, MERGE` | Don't cascade REMOVE if address could be reused |
| Invoice + AuditLog | ❌ No cascade | AuditLog must survive even if Invoice is deleted |
| Parent + Children in a tree structure | `PERSIST, MERGE` | Be careful with REMOVE — could wipe entire subtree |

**@ManyToMany vs Junction Entity — Decision:**
```
Use plain @ManyToMany when:
  ✅ Junction table has NO extra columns (just two FKs)
  ✅ The relationship is simple (tags on a blog post)
  ✅ You'll never need to query "when was this relationship created"

Use a dedicated Junction @Entity when:
  ✅ You need extra data on the relationship (enrollment date, role, status)
  ✅ You need to audit or soft-delete the relationship
  ✅ You query the junction table directly

Real examples:
  Product ↔ Tag          → @ManyToMany (no extra data needed)
  Student ↔ Course       → Junction Entity (need enrollment date, grade)
  User ↔ Role            → Junction Entity (need assigned_by, assigned_at)
  Order ↔ Product        → Junction Entity (this IS OrderItem — has quantity, price)
```

---

## 4. Lazy vs Eager Loading

### 🔰 Basic — Definitions

| Type | When data is loaded | SQL |
|------|--------------------|----|
| **EAGER** | Immediately when parent is loaded | Extra JOIN or separate SELECT fires right away |
| **LAZY** | Only when you actually access the field | Proxy fires a new SELECT on demand |

**Defaults (memorize these!):**

| Relationship | Default |
|-------------|---------|
| `@ManyToOne` | **EAGER** |
| `@OneToOne` | **EAGER** |
| `@OneToMany` | **LAZY** |
| `@ManyToMany` | **LAZY** |

> ⚠️ **Best Practice:** Always use `LAZY` for everything, then use **JOIN FETCH** in queries where you need data. EAGER loading is almost always a mistake.

### ⚙️ Intermediate — N+1 Problem (Most Common Interview Question!)

```java
// BAD: Causes N+1 problem
List<Order> orders = orderRepo.findAll();  // 1 query: SELECT * FROM orders
for (Order o : orders) {
    System.out.println(o.getItems().size()); // N queries: SELECT * FROM items WHERE order_id = ?
}
// If you have 100 orders → 101 SQL queries! 🔥
```

**Fix with JOIN FETCH:**
```java
// JPQL with JOIN FETCH — loads everything in 1 query
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.status = :status")
List<Order> findOrdersWithItems(@Param("status") OrderStatus status);
```

**Fix with @EntityGraph:**
```java
@EntityGraph(attributePaths = {"items", "items.product"})
@Query("SELECT o FROM Order o")
List<Order> findAllWithItems();
```

### 🧠 Advanced — LazyInitializationException

```java
// ❌ This will throw LazyInitializationException
@Transactional
public Order getOrder(Long id) {
    Order order = orderRepo.findById(id).get(); // Session is open
    return order; // Session closes here
}

// In controller or another method (Session already closed):
order.getItems().size(); // 💥 LazyInitializationException!
```

**Solutions:**

```java
// Option 1: Keep session open (use @Transactional on calling method)
@Transactional
public void processOrder(Long id) {
    Order order = orderRepo.findById(id).get();
    order.getItems().forEach(item -> process(item)); // Safe, session open
}

// Option 2: JOIN FETCH in query
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);

// Option 3: DTO Projection — fetch only what you need
@Query("SELECT new com.example.OrderDTO(o.id, o.status, i.productName) " +
       "FROM Order o JOIN o.items i WHERE o.id = :id")
List<OrderDTO> findOrderDetails(@Param("id") Long id);

// Option 4: spring.jpa.open-in-view=true (DEFAULT in Spring Boot!)
// ⚠️ Keep session open for entire HTTP request — ANTI-PATTERN, disable it!
```

> 🎯 **Interview Tip:** `spring.jpa.open-in-view=true` is enabled by default in Spring Boot. Always set it to `false` in production — it keeps a DB connection open for the entire HTTP request, killing performance under load.

### 🏭 Industry Decision — Lazy vs Eager

**How to decide fetch strategy for every relationship:**

```
Ask yourself: "Do I ALWAYS need this data when I load the parent?"

YES → Still use LAZY + JOIN FETCH in specific queries
      (EAGER means every findById() pulls the collection — even when you don't need it)

NO  → Definitely LAZY
```

**Real-world fetch strategy decisions:**

| Entity + Relationship | Strategy | Reason |
|----------------------|----------|--------|
| `Order` → `items` (List) | LAZY | Not every order query needs items; use JOIN FETCH on detail page |
| `OrderItem` → `order` (ManyToOne) | LAZY (override default!) | Default is EAGER — always override to LAZY |
| `User` → `roles` (ManyToMany) | LAZY | Load roles only for security checks, not every user load |
| `Product` → `category` (ManyToOne) | LAZY | Don't load category tree on every product list query |
| `Employee` → `department` (ManyToOne) | LAZY | Load department only on employee detail screen |

**How to handle fetch in different API scenarios:**

```java
// Scenario 1: List API — show 50 orders, only need id, status, total
// → Use DTO projection, don't fetch relationships at all
@Query("SELECT new com.example.OrderListDTO(o.id, o.status, o.total) FROM Order o")
List<OrderListDTO> findAllForList();

// Scenario 2: Detail API — show one order with all items and product info
// → Use JOIN FETCH for exactly what you need
@Query("SELECT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.product WHERE o.id = :id")
Optional<Order> findDetailById(@Param("id") Long id);

// Scenario 3: Dashboard API — show order count and recent orders
// → Paginated LAZY load, no collection fetching
Page<Order> findByCustomerId(Long customerId, Pageable pageable);
```

**N+1 Detection in production:**
```properties
# Enable SQL logging to detect N+1 during development
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Use Hibernate statistics to count queries per request
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG

# Use Datasource Proxy (p6spy) for production-friendly query counting
# If one API call fires > 5 queries → investigate for N+1
```

---

## 5. Transactions

### 🔰 Basic — What is a Transaction?

A transaction groups multiple DB operations so they either **all succeed (COMMIT)** or **all fail (ROLLBACK)** — no partial updates.

```
BEGIN TRANSACTION
  INSERT INTO orders ...
  INSERT INTO order_items ...
  UPDATE inventory ...
COMMIT  ← all or nothing
```

**ACID Properties:**
| Property | Meaning |
|----------|---------|
| **A**tomicity | All or nothing |
| **C**onsistency | DB goes from one valid state to another |
| **I**solation | Concurrent transactions don't see each other's partial work |
| **D**urability | Committed data survives crashes |

### ⚙️ Intermediate — @Transactional Deep Dive

```java
@Service
public class OrderService {

    @Transactional  // Spring manages BEGIN/COMMIT/ROLLBACK for you
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepo.save(order);

        inventoryService.deductStock(request.getItems()); // also participates in same TX

        return order;  // COMMIT happens here
        // If any exception → auto ROLLBACK
    }
}
```

**Where to put @Transactional?**
- ✅ Service layer (business logic)
- ❌ Repository layer (Spring Data already handles it)
- ❌ Controller layer (too high level)

### ⚙️ Intermediate — Propagation Levels

> This is one of the most asked interview topics!

```java
@Transactional(propagation = Propagation.REQUIRED)  // DEFAULT
// Use existing TX if present; create new one if not

@Transactional(propagation = Propagation.REQUIRES_NEW)
// Always create a NEW transaction; suspend existing one
// Use case: audit logging — must persist even if outer TX rolls back

@Transactional(propagation = Propagation.NESTED)
// Creates a savepoint; inner TX can rollback independently

@Transactional(propagation = Propagation.SUPPORTS)
// Join existing TX if present; otherwise run without TX

@Transactional(propagation = Propagation.NOT_SUPPORTED)
// Always run without TX (suspend if exists)

@Transactional(propagation = Propagation.MANDATORY)
// Must have existing TX; throw exception if none

@Transactional(propagation = Propagation.NEVER)
// Must NOT have TX; throw exception if one exists
```

**Real-world example:**
```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder(OrderRequest req) {
        Order order = createOrder(req);     // REQUIRED — same TX
        auditService.log("Order placed");   // REQUIRES_NEW — own TX (won't rollback if order fails)
        notifyInventory(req);               // REQUIRED — same TX
    }
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        // Always saved, even if caller rolls back
        auditRepo.save(new AuditLog(message));
    }
}
```

### 🧠 Advanced — Isolation Levels & Concurrency Problems

**Concurrency Problems:**

| Problem | What happens |
|---------|-------------|
| **Dirty Read** | TX reads uncommitted changes from another TX |
| **Non-Repeatable Read** | Same row read twice gives different values (another TX updated it) |
| **Phantom Read** | Same query run twice gives different rows (another TX inserted/deleted) |

**Isolation Levels (increasing strictness):**

| Level | Dirty Read | Non-Repeatable | Phantom | Performance |
|-------|-----------|----------------|---------|-------------|
| `READ_UNCOMMITTED` | ✅ possible | ✅ possible | ✅ possible | Fastest |
| `READ_COMMITTED` | ❌ prevented | ✅ possible | ✅ possible | Default in most DBs |
| `REPEATABLE_READ` | ❌ | ❌ prevented | ✅ possible | Slower |
| `SERIALIZABLE` | ❌ | ❌ | ❌ prevented | Slowest |

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // safest default
public Order findOrder(Long id) { ... }

@Transactional(isolation = Isolation.SERIALIZABLE)    // for financial operations
public void transferMoney(Long from, Long to, BigDecimal amount) { ... }
```

### 🧠 Advanced — Self-Invocation Trap

```java
@Service
public class OrderService {

    @Transactional
    public void methodA() {
        this.methodB();  // ❌ @Transactional on methodB is IGNORED!
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // Spring AOP proxy is bypassed when calling this.methodB()
        // So REQUIRES_NEW has no effect here!
    }
}
```

**Fix:**
```java
// Option 1: Inject self
@Autowired
private OrderService self;  // inject proxy, not 'this'

public void methodA() {
    self.methodB();  // ✅ goes through proxy
}

// Option 2: Move methodB to a separate @Service
// Option 3: Use ApplicationContext.getBean(OrderService.class)
```

> 🎯 **Interview Tip:** Self-invocation bypass is the #1 @Transactional gotcha. Always mention it.

### 🧠 Advanced — Rollback Rules

```java
// Default: only rolls back on RuntimeException (unchecked)
@Transactional
public void process() throws CheckedException {
    // CheckedException → NO rollback (default!)
}

// Explicit rollback for checked exception:
@Transactional(rollbackFor = Exception.class)
public void process() throws CheckedException { ... }

// Don't rollback for specific runtime exception:
@Transactional(noRollbackFor = OptimisticLockException.class)
public void process() { ... }
```

### 🏭 Industry Decision — Transactions

**Propagation Decision Tree (print this out):**

```
Starting a new operation — which propagation to use?

Is this a business operation that MUST be part of the caller's transaction?
  YES → REQUIRED (default) ✅
  NO, it should always be independent (e.g. audit log, notification record)?
    → REQUIRES_NEW ✅

Is this a helper/utility method that works with or without a transaction?
  → SUPPORTS ✅

Is this a read-only reporting method that must NEVER be inside a transaction?
  → NOT_SUPPORTED ✅ (releases DB connection while executing)

Is this a method that is dangerous to run outside a transaction (e.g. money transfer)?
  → MANDATORY ✅ (throws if no transaction exists — forces callers to be responsible)

Do I need a savepoint (partial rollback without rolling back everything)?
  → NESTED ✅ (only with JDBC transactions, not JTA)
```

**Isolation Level Decision — Real Industry Use:**

| Use Case | Isolation Level | Why |
|----------|----------------|-----|
| Read user profile, product catalog | `READ_COMMITTED` | Default — prevents dirty reads, good enough |
| Generate PDF report (consistent snapshot) | `REPEATABLE_READ` | Same rows every time during report generation |
| Bank transfer, debit/credit operations | `SERIALIZABLE` | No phantom reads, fully consistent — worth the lock cost |
| Real-time analytics dashboard (stale OK) | `READ_UNCOMMITTED` | Max speed, stale data acceptable for dashboards |

**Optimistic vs Pessimistic Locking — When to use which:**

```java
// Optimistic Locking — use when conflicts are RARE (most web apps)
// "Go ahead and update, but fail if someone else changed it first"
@Entity
public class Product {
    @Version
    private Integer version;  // Hibernate checks this on UPDATE
}
// Good for: e-commerce product updates, user profile edits, CMS content

// Pessimistic Locking — use when conflicts are FREQUENT (financial systems)
// "Lock the row while I'm working on it, nobody else can touch it"
@Query("SELECT p FROM Product p WHERE p.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Product> findByIdForUpdate(@Param("id") Long id);
// Good for: ticket booking (one seat, many buyers), bank account balance updates
```

**@Transactional(readOnly = true) — Always use for reads:**
```java
// ✅ Always add readOnly=true for queries — it:
//   1. Disables dirty checking (Hibernate skips tracking changes → faster)
//   2. Tells the DB driver it's a read-only TX (can route to replica DB)
//   3. Prevents accidental writes

@Transactional(readOnly = true)
public List<Order> getOrdersForCustomer(Long customerId) {
    return orderRepo.findByCustomerId(customerId);
}

// In Spring Data repositories, annotate the whole class with readOnly and
// override for write methods:
@Transactional(readOnly = true)
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @Transactional  // override: write methods need readOnly=false
    <S extends Order> S save(S entity);
}
```

---

## 6. Query Optimization

### 🔰 Basic — JPQL vs Native SQL vs Criteria API

```java
// 1. JPQL — object-based, portable, recommended
@Query("SELECT o FROM Order o WHERE o.customer.email = :email")
List<Order> findByCustomerEmail(@Param("email") String email);

// 2. Native SQL — when JPQL can't do it (complex queries, DB-specific functions)
@Query(value = "SELECT * FROM orders WHERE YEAR(created_at) = :year", nativeQuery = true)
List<Order> findByYear(@Param("year") int year);

// 3. Criteria API — dynamic queries, type-safe (verbose but powerful)
public List<Order> searchOrders(OrderSearchCriteria criteria) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Order> cq = cb.createQuery(Order.class);
    Root<Order> root = cq.from(Order.class);

    List<Predicate> predicates = new ArrayList<>();
    if (criteria.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
    }
    if (criteria.getMinAmount() != null) {
        predicates.add(cb.ge(root.get("totalAmount"), criteria.getMinAmount()));
    }
    cq.where(predicates.toArray(new Predicate[0]));
    return em.createQuery(cq).getResultList();
}
```

### ⚙️ Intermediate — Pagination & Sorting

```java
// Spring Data JPA Pagination
Page<Order> findByStatus(OrderStatus status, Pageable pageable);

// Usage:
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
Page<Order> page = orderRepo.findByStatus(PENDING, pageable);

page.getContent();     // List of orders for this page
page.getTotalPages();  // total pages
page.getTotalElements(); // total records
```

**Slice vs Page:**
```java
// Page → runs COUNT(*) query too (for totalElements) — expensive on large tables
Page<Order> findAll(Pageable pageable);

// Slice → no COUNT query — just knows if there's a "next page" — use for infinite scroll
Slice<Order> findByStatus(OrderStatus status, Pageable pageable);
```

### ⚙️ Intermediate — DTO Projections (Massive Performance Win)

```java
// ❌ Fetches entire entity with all columns
List<Order> findAll();

// ✅ Interface-based projection — only fetches needed columns
public interface OrderSummary {
    Long getId();
    String getTrackingNumber();
    OrderStatus getStatus();
    // Hibernate generates: SELECT id, tracking_number, status FROM orders
}
List<OrderSummary> findAllProjectedBy();

// ✅ Class-based DTO projection (JPQL constructor expression)
@Query("SELECT new com.example.OrderDTO(o.id, o.trackingNumber, o.status) FROM Order o")
List<OrderDTO> findOrderSummaries();
```

### 🧠 Advanced — Second-Level Cache

```
L1 Cache (Session / EntityManager) — per transaction, always on
L2 Cache (SessionFactory) — per application, optional, shared across sessions
Query Cache — caches query result sets, used with L2
```

```java
// Enable L2 cache for entity
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)  // Hibernate @Cache
public class Product { ... }

// Configure in application.properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.ehcache.EhCacheRegionFactory
spring.jpa.properties.hibernate.cache.use_query_cache=true
```

**Cache strategies:**

| Strategy | Use When |
|----------|----------|
| `READ_ONLY` | Data never changes (reference data, enums) |
| `READ_WRITE` | Data changes sometimes (most entities) |
| `NONSTRICT_READ_WRITE` | OK with stale reads briefly, better performance |
| `TRANSACTIONAL` | Full transactional guarantee (JTA only) |

### 🧠 Advanced — Batch Processing & Fetch Size

```java
// ❌ Inserts 1000 records with 1000 individual INSERTs
for (Product p : products) {
    productRepo.save(p);
}

// ✅ Batch inserts
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

// For manual batch:
@Transactional
public void bulkInsert(List<Product> products) {
    for (int i = 0; i < products.size(); i++) {
        em.persist(products.get(i));
        if (i % 50 == 0) {
            em.flush();   // write batch to DB
            em.clear();   // clear L1 cache to free memory
        }
    }
}
```

**Fetch Size Optimization:**
```java
// ❌ Fetches 1 row at a time from JDBC cursor
List<Order> findAll();

// ✅ Fetch 500 rows per roundtrip
@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "500"))
List<Order> findAll();
```

### 🧠 Advanced — @StatelessSession for Bulk Operations

```java
// For massive bulk reads/writes — skips L1 cache entirely
StatelessSession session = sessionFactory.openStatelessSession();
Transaction tx = session.beginTransaction();
ScrollableResults<Product> results = session
    .createQuery("FROM Product", Product.class)
    .setFetchSize(1000)
    .scroll(ScrollMode.FORWARD_ONLY);
while (results.next()) {
    Product p = results.get();
    p.setPrice(p.getPrice().multiply(BigDecimal.valueOf(1.1)));
    session.update(p);
}
tx.commit();
session.close();
```

### 🏭 Industry Decision — Query Strategy

**Which query approach for which situation?**

| Situation | Use | Why |
|-----------|-----|-----|
| Simple find by field, Spring Data method names | `findByStatusAndCreatedAtAfter(...)` | Zero boilerplate, automatic |
| Fetch entity with specific relationships | `@Query` + `JOIN FETCH` | Prevent N+1, control exactly what's loaded |
| Complex search with optional filters (search screen) | `Criteria API` or `Specifications` | Dynamic query building, type-safe |
| DB-specific function (JSON ops, window functions) | `@Query(nativeQuery=true)` | JPQL doesn't support DB-specific features |
| Read-only data for API response (list/detail views) | `DTO Projection` (interface or class) | Never load full entity just to serialize it |
| Reporting / aggregation across many tables | `Native SQL` or `JOOQ` | Cleaner, predictable execution plan |
| Paginated list with filters | `Specifications + Pageable` | Reusable, composable filter predicates |
| Infinite scroll / cursor pagination | `Slice<T>` | No COUNT(*) → faster on large tables |
| Numbered pages with total count | `Page<T>` | Runs COUNT(*) — acceptable for small/medium datasets |

**Query method naming vs @Query — Decision:**
```java
// ✅ Use method naming for simple queries (< 3 conditions)
List<Order> findByStatusAndCustomerId(OrderStatus status, Long customerId);

// ✅ Use @Query when method name becomes unreadable
// BAD — impossible to read:
List<Order> findByStatusAndCustomerIdAndCreatedAtBetweenOrderByCreatedAtDesc(...);

// GOOD — use @Query instead:
@Query("SELECT o FROM Order o WHERE o.status = :status AND o.customer.id = :cId " +
       "AND o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
List<Order> findFilteredOrders(...);
```

**When to use Specifications (industry pattern for search APIs):**
```java
// Build reusable, composable query predicates
public class OrderSpecifications {

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAfter(LocalDate date) {
        return (root, query, cb) ->
            date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> forCustomer(Long customerId) {
        return (root, query, cb) ->
            customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }
}

// Usage: dynamically combine
Specification<Order> spec = Specification
    .where(hasStatus(filter.getStatus()))
    .and(createdAfter(filter.getFromDate()))
    .and(forCustomer(filter.getCustomerId()));

Page<Order> results = orderRepo.findAll(spec, pageable);
```

**L2 Cache — When to use in production:**
```
Enable L2 cache for:
  ✅ Reference/lookup data that rarely changes: Country, Currency, Category, Role
  ✅ Configuration tables read on every request
  ✅ Product catalog (read heavy, updated infrequently)

Do NOT use L2 cache for:
  ❌ Order, Payment, User (mutable, read your own writes required)
  ❌ Inventory quantities (must always be fresh — stale cache = overselling!)
  ❌ Any entity modified by multiple services in a distributed system
     (cache invalidation across services is a nightmare)

Industry pattern: Use Redis for distributed caching instead of Hibernate L2
in microservices — @Cacheable from Spring Cache is cleaner and service-agnostic.
```

---

## 7. AOP — Aspect-Oriented Programming

### 🔰 Basic — The Problem AOP Solves

```java
// Without AOP — cross-cutting concerns scattered everywhere:
public class OrderService {
    public Order createOrder(OrderRequest req) {
        log.info("Creating order...");         // logging
        checkSecurity();                        // security
        long start = System.currentTimeMillis(); // performance
        // actual business logic
        Order order = ...;
        auditTrail.record(order);              // auditing
        long end = System.currentTimeMillis();
        log.info("Took: " + (end-start) + "ms"); // performance
        return order;
    }
}
```

**AOP extracts these cross-cutting concerns into one place:**
- Logging
- Security
- Transaction management
- Caching
- Performance monitoring
- Exception handling
- Auditing

### ⚙️ Intermediate — AOP Terminology

| Term | Meaning | Analogy |
|------|---------|---------|
| **Aspect** | The class containing cross-cutting logic | The "plug-in" |
| **Advice** | The actual code to execute (method in Aspect) | The "action" |
| **Pointcut** | Expression defining WHERE advice applies | The "filter" |
| **JoinPoint** | A specific point in execution (method call) | The "moment" |
| **Weaving** | Applying aspects to target objects | The "injection" |
| **Proxy** | Object wrapping the target, intercepts calls | The "middleman" |

### ⚙️ Intermediate — Advice Types

```java
@Aspect
@Component
public class LoggingAspect {

    // BEFORE — runs before the method
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint jp) {
        log.info("Calling: {}", jp.getSignature().getName());
    }

    // AFTER RETURNING — runs after successful return
    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) {
        log.info("Method {} returned: {}", jp.getSignature().getName(), result);
    }

    // AFTER THROWING — runs if method throws exception
    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void logAfterThrowing(JoinPoint jp, Exception ex) {
        log.error("Exception in {}: {}", jp.getSignature().getName(), ex.getMessage());
    }

    // AFTER (FINALLY) — runs always (like finally block)
    @After("execution(* com.example.service.*.*(..))")
    public void logAfter(JoinPoint jp) {
        log.info("Method {} completed", jp.getSignature().getName());
    }

    // AROUND — wraps the entire method (most powerful)
    @Around("execution(* com.example.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        log.info("START: {}", pjp.getSignature());
        try {
            Object result = pjp.proceed();  // ← actually execute the method
            log.info("SUCCESS in {}ms", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("FAILED: {}", e.getMessage());
            throw e;
        }
    }
}
```

**Execution order of advice types:**
```
→ @Around (before proceed)
  → @Before
    → [actual method]
  → @AfterReturning / @AfterThrowing
  → @After
→ @Around (after proceed)
```

### ⚙️ Intermediate — Pointcut Expressions

```java
// Syntax: execution(modifiers? returnType declaring-type? method-name(params) throws?)

// All methods in service package
@Pointcut("execution(* com.example.service.*.*(..))")

// All public methods anywhere
@Pointcut("execution(public * *(..))")

// Methods returning String
@Pointcut("execution(String com.example..*(..))")

// Methods with specific annotation
@Pointcut("@annotation(com.example.Auditable)")

// Methods on a specific class
@Pointcut("within(com.example.service.OrderService)")

// Method with specific argument type
@Pointcut("args(com.example.dto.OrderRequest)")

// Combine with && || !
@Pointcut("execution(* com.example.service.*.*(..)) && !execution(* com.example.service.*.find*(..))")
```

### 🧠 Advanced — Custom Annotation-Driven AOP

**Real-world example: Method-level audit logging**

```java
// Step 1: Define custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
}

// Step 2: Use it on methods
@Service
public class OrderService {

    @Auditable(action = "CREATE_ORDER")
    @Transactional
    public Order createOrder(OrderRequest req) { ... }

    @Auditable(action = "CANCEL_ORDER")
    @Transactional
    public void cancelOrder(Long id) { ... }
}

// Step 3: Create the Aspect
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditRepo;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String action = auditable.action();
        LocalDateTime timestamp = LocalDateTime.now();

        try {
            Object result = pjp.proceed();
            // Log success
            auditRepo.save(new AuditLog(user, action, timestamp, "SUCCESS"));
            return result;
        } catch (Exception e) {
            // Log failure
            auditRepo.save(new AuditLog(user, action, timestamp, "FAILURE: " + e.getMessage()));
            throw e;
        }
    }
}
```

### 🧠 Advanced — JDK Proxy vs CGLIB Proxy

| | JDK Dynamic Proxy | CGLIB Proxy |
|--|-------------------|-------------|
| **Requires** | Interface | Concrete class |
| **How** | Implements interface | Subclasses the class |
| **When** | Bean implements interface | Bean has no interface |
| **Limitation** | Only works on interface methods | Can't proxy `final` classes/methods |

```java
// Spring Boot default: use CGLIB for all beans (since Spring 4)
@SpringBootApplication
// or explicitly:
@EnableAspectJAutoProxy(proxyTargetClass = true)  // CGLIB
@EnableAspectJAutoProxy(proxyTargetClass = false) // JDK Proxy (needs interface)
```

> 🎯 **Interview Tip:** This is why `@Transactional` doesn't work on `final` methods or when you use `this.method()` — CGLIB can't proxy final methods, and `this` bypasses the proxy entirely.

### 🧠 Advanced — AOP Pitfalls Summary

```java
// ❌ Pitfall 1: final methods can't be proxied by CGLIB
@Transactional
public final void process() { ... }  // @Transactional IGNORED!

// ❌ Pitfall 2: self-invocation bypasses proxy
@Service
public class MyService {
    @Transactional
    public void outer() {
        this.inner();  // inner's @Transactional is IGNORED
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() { ... }
}

// ❌ Pitfall 3: AOP only works on Spring-managed beans
MyService svc = new MyService();  // plain Java object, no proxy
svc.process();  // @Transactional IGNORED

// ❌ Pitfall 4: private methods are never proxied
@Transactional
private void doSomething() { ... }  // IGNORED
```

### 🏭 Industry Decision — AOP

**Which Advice type for which use case?**

| Use Case | Advice Type | Why |
|----------|------------|-----|
| Log method entry (method name, args) | `@Before` | Runs before, simple, no control needed |
| Log method success + return value | `@AfterReturning` | Accesses return value cleanly |
| Log exceptions / send alerts | `@AfterThrowing` | Triggered only on exception |
| Cleanup (close resource, reset context) | `@After` | Like finally — always runs |
| Performance timing (start → end) | `@Around` | Only advice that can wrap both sides |
| Retry on failure | `@Around` | Can catch exception and call `proceed()` again |
| Feature flag check (skip method body) | `@Around` | Can skip `proceed()` entirely and return default |
| Cache method results | `@Around` | Check cache first; call proceed() only on miss |
| Validate input before method | `@Before` | Simple pre-check |
| Audit who did what and result | `@Around` | Captures actor, action, result, and exception |

**When to use AOP vs manual code:**

```
Use AOP when:
  ✅ The same concern repeats across 10+ methods/classes
  ✅ It's non-business logic (logging, security, metrics, retry)
  ✅ You want to add behavior WITHOUT modifying existing classes (Open/Closed Principle)
  ✅ You want to add behavior to third-party or legacy code you can't change

Do NOT use AOP when:
  ❌ It's core business logic — keep business rules explicit and traceable
  ❌ Only 1–2 methods need it — just call the method directly
  ❌ The behavior depends heavily on method-specific context — too tightly coupled
  ❌ Team is not familiar with AOP — debugging proxy issues is painful

Real industry usage examples:
  @Transactional          → Spring's own AOP (you use it every day!)
  @PreAuthorize           → Spring Security AOP (method-level security)
  @Cacheable              → Spring Cache AOP
  @Retryable              → Spring Retry AOP
  Custom @RateLimited     → Build your own with @Around
  Custom @Auditable       → Build your own with @Around
  Custom @ExecutionTime   → Build your own with @Around
```

**AOP in Microservices — Practical patterns:**
```java
// Pattern 1: Centralized execution time logging across all services
@Around("execution(* com.example..service.*.*(..))")
public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();
    Object result = pjp.proceed();
    long duration = System.currentTimeMillis() - start;
    // Push to Micrometer/Prometheus for dashboards
    meterRegistry.timer("method.execution", "method", pjp.getSignature().getName())
                 .record(duration, TimeUnit.MILLISECONDS);
    return result;
}

// Pattern 2: Distributed tracing context propagation
@Before("execution(* com.example..service.*.*(..))")
public void propagateTraceContext(JoinPoint jp) {
    MDC.put("traceId", TraceContext.getCurrentTraceId());
    MDC.put("spanId", UUID.randomUUID().toString());
}

// Pattern 3: Input validation aspect
@Before("@annotation(validated)")
public void validate(JoinPoint jp, Validated validated) {
    Object[] args = jp.getArgs();
    for (Object arg : args) {
        Set<ConstraintViolation<Object>> violations = validator.validate(arg);
        if (!violations.isEmpty()) throw new ValidationException(violations.toString());
    }
}
```

---

## 8. Quick-Fire Interview Q&A Cheatsheet

### JPA / Hibernate

| Question | Answer |
|----------|--------|
| JPA vs Hibernate? | JPA = spec, Hibernate = implementation |
| What is PersistenceContext? | In-memory 1st level cache per EntityManager/Session |
| persist() vs merge() vs save()? | persist→new entity; merge→detached entity; save→Hibernate only, returns ID |
| What is dirty checking? | Hibernate auto-detects changes to managed entities and syncs at flush |
| When does Hibernate flush? | Before queries, on TX commit, when explicitly called |
| What triggers LazyInitializationException? | Accessing lazy collection after session closes |
| How to fix N+1? | JOIN FETCH in JPQL or @EntityGraph |
| SINGLE_TABLE vs JOINED inheritance? | Single=fast, nulls; Joined=clean schema, JOINs |
| What is orphanRemoval? | Delete child entity when removed from parent collection |
| What is @Version? | Optimistic locking — prevents lost updates in concurrent TX |

### Transactions

| Question | Answer |
|----------|--------|
| @Transactional default behavior? | PROPAGATION.REQUIRED, ISOLATION.DEFAULT, rollback on RuntimeException only |
| REQUIRED vs REQUIRES_NEW? | REQUIRED joins existing TX; REQUIRES_NEW suspends it and creates new one |
| Why no rollback on checked exceptions? | Historical EJB decision; use rollbackFor = Exception.class to fix |
| Self-invocation problem? | Calling @Transactional method via `this` bypasses Spring proxy |
| Where to place @Transactional? | Service layer; not controller or repository |
| READ_COMMITTED vs REPEATABLE_READ? | RC prevents dirty reads; RR also prevents non-repeatable reads |

### AOP

| Question | Answer |
|----------|--------|
| What is AOP? | Separating cross-cutting concerns (log, security, TX) from business logic |
| CGLIB vs JDK Proxy? | JDK needs interface; CGLIB subclasses (can't proxy final) |
| @Around vs @Before? | Around wraps entire method + controls proceed(); Before just runs before |
| Can AOP proxy private methods? | No — proxy can't intercept private methods |
| How does @Transactional work internally? | Spring creates AOP proxy; proxy wraps method in begin/commit/rollback |
| @EnableAspectJAutoProxy does what? | Enables Spring's AOP proxy mechanism for @Aspect beans |

---

## 🔑 Golden Rules to Always Mention in Interviews

1. **Always use LAZY loading** — never default to EAGER; use JOIN FETCH when needed
2. **N+1 is your enemy** — detect with Hibernate statistics, fix with JOIN FETCH
3. **@Transactional on service layer only** — never controller
4. **Self-invocation kills @Transactional** — always mention this gotcha
5. **Turn off open-in-view** — `spring.jpa.open-in-view=false`
6. **DTO projections over full entity loads** — for read-heavy screens
7. **Batch inserts with flush + clear** — never loop-save 10k records one by one
8. **Checked exceptions don't auto-rollback** — always add `rollbackFor = Exception.class`
9. **AOP can't proxy final methods or private methods**
10. **mappedBy side never owns the FK** — the @JoinColumn side does

---

> 💡 **Pro tip for the interview:** Don't just list features — tell a story about when you used it, what problem it solved, and what gotcha you ran into. That's what separates a 5-year developer from the rest.

---

## 9. 🏭 When to Use What — Industry Decision Guide

> **The ultimate cheatsheet.** One place to find every "when to use what" decision across all topics. Bookmark this section.

---

### 🗂️ ORM Technology Choice

```
Building a new Spring Boot service — what data layer?

Does it have complex domain with rich relationships (Order, User, Product, Payment)?
  YES → JPA + Hibernate + Spring Data JPA ✅

Is it a reporting/analytics service with complex SQL?
  YES → JOOQ or Native SQL via JdbcTemplate ✅

Is it a simple CRUD service with flat data model?
  YES → Spring Data JDBC (lighter than Hibernate) ✅

Is it a bulk ETL / data pipeline processing millions of rows?
  YES → Spring Batch + JdbcTemplate or @StatelessSession ✅

Is the DB non-relational (MongoDB, Redis, Cassandra)?
  YES → Spring Data MongoDB / Redis / Cassandra ✅
```

---

### 🆔 ID Generation Strategy

| When | Use |
|------|-----|
| MySQL / MariaDB | `IDENTITY` |
| PostgreSQL (need batch inserts) | `SEQUENCE` |
| Oracle | `SEQUENCE` |
| Microservices / distributed ID | `UUID` |
| Prototyping only | `AUTO` |
| Never in production | `TABLE` |

---

### 🏗️ Inheritance Strategy

| When | Use |
|------|-----|
| Few subtypes, polymorphic queries, performance first | `SINGLE_TABLE` |
| Clean normalized schema matters, JOINs acceptable | `JOINED` |
| Each subtype queried independently, no polymorphic FK needed | `TABLE_PER_CLASS` |
| Object is a value object (no own ID, always owned) | `@Embeddable` |

---

### 🔗 Relationship Type

| When | Use |
|------|-----|
| One entity has one dependent (User → Profile) | `@OneToOne` |
| Parent owns many children (Order → Items) | `@OneToMany` + `@ManyToOne` |
| Many-to-many, no extra data on join | `@ManyToMany` |
| Many-to-many WITH extra data on join (date, status) | Dedicated junction `@Entity` |
| Self-referencing (Employee → Manager) | `@ManyToOne` self-join |

---

### 🔁 Cascade Type

| When | Use |
|------|-----|
| Children have no meaning without parent (Order → Items) | `ALL` + `orphanRemoval = true` |
| Create child automatically with parent | `PERSIST` |
| Update child when parent is merged | `MERGE` |
| Audit logs, independent entities | ❌ No cascade |
| Shared child referenced by multiple parents | ❌ No cascade (don't delete shared data) |

---

### 📦 Fetch Strategy

| When | Use |
|------|-----|
| Always (default everything) | `LAZY` |
| Need data in same transaction | `LAZY` + `JOIN FETCH` |
| Need data across multiple screens selectively | `LAZY` + `@EntityGraph` per query |
| Read-only API response (no lazy needed) | `DTO Projection` — skip entity loading entirely |
| NEVER use in production | `EAGER` |

---

### 📝 Query Type

| When | Use |
|------|-----|
| Simple field-based lookup | Spring Data method naming |
| Fixed query, no dynamic filters | `@Query` JPQL |
| Dynamic multi-filter search screen | `Specification` + `Pageable` |
| DB-specific functions, window functions | `@Query(nativeQuery = true)` |
| Complex multi-table reporting | Native SQL / JOOQ |
| API list response (no full entity needed) | Interface/DTO Projection |
| Numbered pages with total count | `Page<T>` |
| Infinite scroll / cursor-based | `Slice<T>` |
| Bulk read/process (millions of rows) | `@StatelessSession` + `ScrollableResults` |

---

### ⚙️ Transaction Propagation

| When | Use |
|------|-----|
| Normal service method (standard) | `REQUIRED` ✅ |
| Audit log, notification (must always save) | `REQUIRES_NEW` ✅ |
| Read-only query (always) | `REQUIRED` + `readOnly = true` ✅ |
| Helper that works with or without TX | `SUPPORTS` |
| Must enforce caller has TX (money transfer) | `MANDATORY` |
| Batch job step (partial rollback OK) | `NESTED` |
| Method that must never run in TX | `NOT_SUPPORTED` |

---

### 🔐 Isolation Level

| When | Use |
|------|-----|
| Standard read (profile, product, order status) | `READ_COMMITTED` |
| Report generation (consistent snapshot) | `REPEATABLE_READ` |
| Financial operations (debit/credit, transfers) | `SERIALIZABLE` |
| Real-time analytics (stale OK) | `READ_UNCOMMITTED` |

---

### 🔒 Locking Strategy

| When | Use |
|------|-----|
| Conflicts rare (most web apps, CMS, profiles) | Optimistic locking (`@Version`) |
| Conflicts frequent (ticket booking, bank balance) | Pessimistic locking (`PESSIMISTIC_WRITE`) |
| Read that must see latest committed value | Pessimistic Read (`PESSIMISTIC_READ`) |

---

### 🎯 AOP Advice Type

| When | Use |
|------|-----|
| Log method input / validate before | `@Before` |
| Log success + return value | `@AfterReturning` |
| Handle / log exception | `@AfterThrowing` |
| Always clean up (close resource, reset MDC) | `@After` |
| Performance timing, retry, caching, feature flag | `@Around` |
| Audit (who, what, result, exception) | `@Around` |

---

### 🧩 AOP vs Manual Code

| When | Use |
|------|-----|
| Same concern in 10+ methods across multiple classes | AOP ✅ |
| Cross-cutting: logging, security, metrics, retry | AOP ✅ |
| Business logic, domain rules | Manual code ✅ (keep business logic explicit) |
| Only 1–2 methods need it | Manual code ✅ (not worth AOP overhead) |

---

### 🚦 Real Microservice Architecture Pattern (Summary)

```
Order Service (complex domain)
  ├── JPA + Hibernate              ← rich domain model
  ├── LAZY everywhere               ← no surprise queries
  ├── JOIN FETCH per use case       ← controlled loading
  ├── DTO projections for APIs      ← only send what's needed
  ├── @Transactional(readOnly=true) ← for all reads
  ├── REQUIRES_NEW for audit logs   ← always persist audit
  ├── Optimistic locking (@Version) ← concurrent order updates
  └── @Around for execution metrics ← AOP for observability

Reporting Service (analytics)
  ├── Native SQL / JOOQ             ← complex queries
  ├── No Hibernate entity tracking  ← StatelessSession or JdbcTemplate
  ├── Page<T> with Slice for scroll ← efficient pagination
  └── Redis @Cacheable              ← cache heavy reports

Notification Service (simple)
  ├── Spring Data JDBC              ← no ORM needed
  ├── REQUIRES_NEW propagation      ← independent TX per notification
  └── @Retryable (AOP)             ← auto-retry on failure
```
