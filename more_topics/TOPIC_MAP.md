## Project Topic Map

Small guide showing **which files correspond to which topics**, so you know what to open for revision.

---

### **1. Java OOP Basics – Shapes (`src/Java_Oops_Example`)**

- **`Shape.java`**: abstract base class, encapsulation (`private` fields), abstract method `area()`, `toString()`, `static` counter.
- **`Circle.java`**: inherits `Shape`, implements `Drawable`, method overriding, method overloading (`resize`), use of `super(...)`.
- **`Rectangle.java`**: inherits `Shape`, implements `Drawable`, overriding `area()` and `draw()`.
- **`Drawable.java`**: simple interface (`void draw()`), used to show interface + polymorphism.
- **`Main.java`**: creates a `List<Shape>`, demonstrates runtime polymorphism, `instanceof`, interface casting, and static method `Shape.getShapeCount()`.

---

### **2. Java Basics / Complete Notes – Expense Tracker (`src/Java_Complete_Notes`)**

- **`Main.java`**: console menu app; covers loops, `switch`, user input (`Scanner`), basic exception handling, and calling service methods.
- **`Expense.java`**: POJO with encapsulation (getters/setters), constructor with `this`, `toString()`, and implements `Printable` to show interfaces.
- **`ExpenseManager.java`**: business logic; uses `ArrayList<Expense>`, method overloading (`filterByCategory(Category)` / `filterByCategory(String)`), loops, and aggregation of `Expense`.
- **`Category.java`**: `enum` for categories (FOOD, TRAVEL, BILLS, OTHER).
- **`Printable.java`**: interface with `printSummary()`, used by `Expense` for polymorphic printing.

---

### **3. JVM Memory Deep Dive (`src/JVM_Memory/JvmMemoryDemo.java`)**

- **`JvmMemoryDemo.java`** (single file, multiple methods):
  - **`stringPoolDemo()`**: shows string pool vs heap strings (`==` vs `.equals()`, `intern()`).
  - **`growStack(int depth)`**: recursive calls + local array to demonstrate stack frames and `StackOverflowError`.
  - **`fillHeap()`**: allocates many `byte[]` blocks into a static list to simulate a heap “leak” and `OutOfMemoryError`.
  - **`main(...)`**: calls the above pieces in order and explains when objects become eligible for GC.

---

### **4. Modern Multithreading – All Concepts via Producer–Consumer (`src/Java_Multithreading`)**

- **`Main.java`**: central entry point.
  - Runs producer–consumer using plain `Thread` / `Runnable` / `synchronized` / `wait`–`notifyAll` and a `volatile` stop flag.
  - Then reuses the same idea with `ExecutorService` + `Callable` + `Future` (`runExecutorServiceDemo()`).
  - Also contains a small `CompletableFuture` pipeline (`runCompletableFutureDemo()`).

- **`TaskQueue.java`**: shared bounded queue and stats.
  - `putTask` / `takeTask`: `synchronized` methods using `wait()` / `notifyAll()` for classic producer–consumer.
  - Uses a `ConcurrentHashMap` to track “tasks processed per consumer thread”.
  - Protects stats with `ReadWriteLock` (`recordProcessed`, `printStats`).

- **`ProducerThread.java`**: extends `Thread`.
  - Simple producer loop that adds 5 tasks into `TaskQueue`, using `Thread.sleep` to simulate work.

- **`ConsumerWorker.java`**: implements `Runnable`.
  - Uses a `volatile` `isRunning` flag so `Main` can stop it safely.
  - Calls `TaskQueue.takeTask()` in a loop, simulating work with `sleep` and responding to interruption.
  - Demonstrates `AtomicInteger` and a `ReentrantLock`-protected counter for atomicity vs explicit locking.
  - Records per-thread stats in `TaskQueue` to demonstrate concurrent collections + `ReadWriteLock`.

---

### **5. Earlier Multithreading Examples (`src/MultiThreading`)**

- **`Synchronized.java`**:  
  - `BankAccount` – instance (`object`) level `synchronized` lock.  
  - `Atm` – `static synchronized` methods showing class-level locking.  
  - `sharedBuffer` – simple producer–consumer with `wait()` / `notify()` on a single slot.

- **`LockInterface.java`**:  
  - `SbiBankAccount` – different `ReentrantLock` usages: `lock()`, `tryLock()`, `tryLock(timeout)`, and `lockInterruptibly()`.  
  - `AccountBalance` – `ReadWriteLock` example with separate read and write locks.

- **`ThreadPool.java`**:  
  - `ExecutorService` basics: `execute()` vs `submit()`, `Future<?>` for `Runnable`, `Future<V>` for `Callable`, and `get()` vs `isDone()` vs `shutdown()`.

---

### **6. Spring Boot Core & Bean Lifecycle – Game Store App (`src/SpringBoot_Complete_Guide/GameStore`)**

- **`GameStoreApplication.java`** (`com.example.gamestore`)  
  - `@SpringBootApplication` main entry point, shows Boot startup and component scanning.  
  - Defines a `CommandLineRunner` that wires together `GamerService`, `GameSessionManager`, `AnalyticsService`, and `StoreInfoService` to demonstrate DI, scopes, `@Lazy`, and property injection.

- **IoC, Beans, DI, Loose Coupling**  
  - `console/Console.java` – interface representing a game console (loose coupling target).  
  - `console/Ps4Console.java`, `console/Ps5Console.java` – implementations of `Console`, with `Ps5Console` marked `@Primary` to resolve multiple beans.  
  - `console/MockConsole.java` – `Console` implementation active only in `test` profile (`@Profile("test")`).  
  - `notification/NotificationService.java` – interface; implemented by `EmailNotificationService` (`@Primary`) and `SmsNotificationService`.  
  - `gamer/HelperService.java` – simple helper `@Service`.  
  - `gamer/GamerService.java` – demonstrates:
    - Constructor injection of `Console` (recommended DI).  
    - Setter injection of `NotificationService` with `@Qualifier("smsNotificationService")`.  
    - Field injection of `HelperService`.  
    - Shows tight vs loose coupling via use of interfaces and `@Primary/@Qualifier`.
  - `notification/InjectionDemo.java` – compares `@Autowired` (by type) vs `@Resource(name="smsNotificationService")` (by name) with a `@PostConstruct` log.

- **Scopes, Prototype in Singleton, @Lazy**  
  - `session/GameSession.java` – `@Component` + `@Scope("prototype")`, each instance has a unique ID.  
  - `session/GameSessionManager.java` – singleton `@Service` that:
    - Holds one directly injected `GameSession` (shows prototype-in-singleton problem).  
    - Uses `ObjectProvider<GameSession>` to create fresh prototypes on demand in `demoSessions()`.  
  - `analytics/AnalyticsService.java` – `@Service @Lazy`; constructor logs when created, `analyze()` called from the runner to demonstrate lazy initialization.

- **Bean Lifecycle, Post-Processors, BeanFactoryPostProcessor** (`lifecycle` package)  
  - `lifecycle/LifecycleConsole.java` – full lifecycle demo bean:
    - Implements `BeanNameAware`, `ApplicationContextAware`, `InitializingBean`, `DisposableBean`.  
    - Uses `@PostConstruct` and `@PreDestroy` to log the exact lifecycle order.  
  - `lifecycle/LoggingBeanPostProcessor.java` – `BeanPostProcessor` that logs before/after initialization for **every** bean (including auto-configured ones).  
  - `lifecycle/GameSessionBeanFactoryPostProcessor.java` – `BeanFactoryPostProcessor` that modifies the `gameSession` `BeanDefinition` (scope) before any instances are created, demonstrating metadata-level customization.

- **@Bean init/destroy methods & Config** (`config` package)  
  - `config/ConsoleEngine.java` – simple class with `startEngine()` and `stopEngine()` methods.  
  - `config/AppConfig.java` – `@Configuration` with a `@Bean` `consoleEngine` that sets `initMethod="startEngine"` and `destroyMethod="stopEngine"`, showing alternative lifecycle hooks via `@Bean` attributes.

- **Configuration Properties & @Value** (`store` package)  
  - `main/resources/application.properties` – defines `app.name` and `app.max-players` (and app name).  
  - `store/StoreInfoService.java` – `@Component` that injects these properties with `@Value` and prints them via `displayStoreInfo()`.

> Note: The embedded Tomcat server and `DispatcherServlet` are visible in logs when running `GameStoreApplication`; you can add a small `@RestController` under `web` later if you want to see request handling end-to-end.

---

### **How to use this map**

- **OOP revision**: open files under `src/Java_Oops_Example`.
- **Core Java / language basics**: use the expense tracker under `src/Java_Complete_Notes`.
- **JVM internals (stack/heap/GC/string pool)**: study `src/JVM_Memory/JvmMemoryDemo.java`.
- **Multithreading (modern, all concepts)**: follow the 4 files under `src/Java_Multithreading` (`Main`, `TaskQueue`, `ProducerThread`, `ConsumerWorker`) – everything is integrated there.
- **Extra multithreading lock/thread-pool variations**: check the older examples in `src/MultiThreading`.
- **Spring Core & Boot internals**: use the Game Store app under `src/SpringBoot_Complete_Guide/GameStore` – especially `GameStoreApplication`, `gamer`, `console`, `notification`, `session`, `analytics`, `lifecycle`, `config`, and `store` packages.

