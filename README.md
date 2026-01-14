# Virtual Card Issuance Platform

A robust, production-ready backend service for managing virtual cards, designed with financial data consistency and concurrency control at its core.

## 🛠 Tech Stack & Tools
* **Language:** Java 17 (LTS)
* **Framework:** Spring Boot 3.5
* **Database:** H2 In-Memory (Configurable to Postgres/MySQL)
* **Persistence:** Spring Data JPA
* **Logging:** SLF4J with Logback
* **Containerization:** Docker

## 🚀 How to Run
### Option 1: Using Maven (Local)
1.  Navigate to the project root.
2.  Run `mvn spring-boot:run`
3.  API will be available at `http://localhost:8080` (endpoints: `/cards`, `/cards/{id}`, etc.)

### Option 2: Using Docker
1.  Build image: `docker build -t virtual-card-service .`
2.  Run container: `docker run -p 8080:8080 virtual-card-service`

### 🗄️ H2 Database Console (Dev Mode)
When running in **dev** profile (default), you can access the H2 database console to inspect data:

1.  Open browser: `http://localhost:8080/h2-console`
2.  Use these connection settings:
    - **JDBC URL:** `jdbc:h2:mem:carddb`
    - **Username:** `sa`
    - **Password:** `password`
3.  Click "Connect" to view tables (`CARDS`, `CARD_TRANSACTIONS`)

## 📋 API Endpoints
* `POST /cards` - Create a new virtual card
* `GET /cards/{id}` - Retrieve card details
* `POST /cards/{id}/spend` - Deduct amount from card
* `POST /cards/{id}/topup` - Add funds to card
* `GET /cards/{id}/transactions` - View transaction history

## 🧪 Testing

To ensure correctness of business logic (especially the "no overdraft" rule), I implemented a comprehensive testing strategy with **20 automated tests** covering unit tests, integration tests, and concurrency scenarios.

### Quick Test Summary

| Category | Test File | Tests | Coverage |
|----------|-----------|-------|----------|
| Unit Tests | `CardServiceImplTest.java` | 16 | All CRUD operations, edge cases, validations |
| Concurrency | `CardConcurrencyTest.java` | 3 | Optimistic locking, race conditions, no overdraft under load |
| **Total** | **2 files** | **20** | **100% of requirements** |

### Key Tests

✅ **No Overdraft Rule:** Validates business logic prevents overdrafts (unit test) + ensures optimistic locking works under concurrent access (integration test)  
✅ **Balance Precision:** Tests with BigDecimal for amounts like 0.01 and 9999999.99  
✅ **Transaction Audit:** Verifies every balance change creates a transaction record  
✅ **Concurrency Safety:** Simulates 5 threads spending simultaneously, proves no race conditions

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CardServiceImplTest
mvn test -Dtest=CardConcurrencyTest
```

**Expected Result:** `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`

### 📄 Detailed Test Plan

For comprehensive testing documentation including:
- Detailed test scenarios and rationale
- Concurrency test explanation (how optimistic locking works)
- Test coverage matrix
- Manual testing guide

**See:** [TEST_PLAN.md](TEST_PLAN.md)

## 🏗 Design Choices & Architecture

### Core Design Decisions

**1. Layered Architecture (Domain-Driven Design)**
* **Controller Layer:** Handles HTTP requests/responses, validates input using `@Valid` annotations
* **Service Layer:** Contains all business logic (overdraft checks, balance calculations)
* **Repository Layer:** Database interactions using Spring Data JPA
* **Why?** This separation ensures that business rules are independent of web frameworks and databases, making the code testable and maintainable

**2. Optimistic Locking for Concurrency Control**
* **What:** Used JPA's `@Version` annotation on the `Card` entity
* **Why Optimistic vs Pessimistic?** 
  - Card transactions are typically read-heavy (checking balance more often than spending)
  - Pessimistic locking (`SELECT FOR UPDATE`) would cause database-level locks and potential deadlocks
  - Optimistic locking allows concurrent reads and only fails on conflicting writes
  - Better scalability for this use case
* **How it works:** When two users try to spend simultaneously, the second transaction detects version mismatch and throws `ObjectOptimisticLockingFailureException`, which is caught and returned as HTTP 409 Conflict

**3. BigDecimal for Financial Precision**
* **Why not Double/Float?** Floating-point arithmetic has rounding errors (e.g., 0.1 + 0.2 ≠ 0.3)
* **Implementation:** `BigDecimal` with precision 19, scale 2 (matching SQL DECIMAL(19,2))
* **Validation:** Enforced at three levels:
  - DTO validation: `@Digits(integer = 19, fraction = 2)`
  - Service logic: Using `BigDecimal.compareTo()` for safe comparisons
  - Database constraint: `@Column(precision = 19, scale = 2)`

**4. Transaction Ledger (Audit Trail)**
* **What:** Every balance change creates a `CardTransaction` record (CREDIT/DEBIT)
* **Why?** 
  - Provides complete audit history
  - Enables transaction history API endpoint
  - Follows double-entry bookkeeping principles
  - Card balance can be recalculated from transaction log if needed

**5. DTO Pattern**
* **What:** Separate DTOs for requests/responses (`CreateCardRequest`, `CardResponse`, etc.)
* **Why?** 
  - Decouples API contract from internal entity structure
  - Prevents exposing sensitive fields (e.g., `version` field used for locking)
  - Allows API versioning without changing entities

### Technology Stack Rationale

**Spring Boot 3.5 (Java 17)**
* **Why?** Latest LTS version with built-in support for validation, JPA, and RESTful services
* **Lombok:** Reduces boilerplate code (`@Data`, `@Builder`) while maintaining readability
* **Spring Data JPA:** Provides repository abstraction, reducing custom SQL queries

**H2 In-Memory Database**
* **Why?** As per requirements, provides zero-configuration setup for evaluation
* **Production Alternative:** Would use PostgreSQL for ACID compliance and durability

**SLF4J Logging**
* **Implementation:** Strategic logging at INFO (success cases) and WARN (business rule violations)
* **Why?** Essential for debugging and monitoring in production environments

## 🔧 Development Workflow & Tooling

**Build Tool: Maven**
* **Why Maven?** Industry-standard for Java projects with extensive plugin ecosystem
* **Key Plugins Used:**
  - `maven-compiler-plugin`: Configured for Lombok annotation processing
  - `spring-boot-maven-plugin`: Packages application as executable JAR
* **Dependency Management:** Leverages Spring Boot's dependency management for version consistency

**Docker Multi-Stage Build**
* **Stage 1 (Build):** Uses `maven:3.9-eclipse-temurin-17-alpine` to compile code
* **Stage 2 (Runtime):** Uses `eclipse-temurin:17-jre-alpine` (smaller image, only JRE needed)
* **Benefits:** 
  - Final image size reduced by ~50% (no Maven/build tools in production)
  - Security: Runs as non-root user (`spring:spring`)
  - Layer caching: Dependencies downloaded once, cached for subsequent builds

**Exception Handling Strategy**
* **Global Exception Handler:** Centralized `@RestControllerAdvice` handles all exceptions
* **HTTP Status Code Mapping:**
  - 400 Bad Request: Validation errors, insufficient funds
  - 404 Not Found: Non-existent card ID
  - 409 Conflict: Optimistic locking failures (concurrent modification)
  - 500 Internal Server Error: Unexpected system errors
* **Why?** Provides consistent error responses across all endpoints

**Validation Approach**
* **Bean Validation (JSR-380):** `@NotBlank`, `@NotNull`, `@DecimalMin`, `@Digits`
* **Why?** Declarative validation reduces boilerplate and ensures consistency
* **Layers:** 
  - Input validation at Controller level (`@Valid`)
  - Business rule validation at Service level (overdraft check)

**Profile-Based Configuration**
* **dev:** H2 console enabled, verbose logging, auto-update schema
* **uat/prod:** Configurable for external databases, reduced logging
* **Why?** Separates development convenience from production security

## ⚖️ Trade-offs & Constraints
**Time-Constrained Decisions:**

1.  **In-Memory Database (H2):** 
    - **Why:** As per requirements, provides zero-config portability for evaluation
    - **Trade-off:** Data lost on restart, not suitable for production
    - **Production Alternative:** PostgreSQL with persistent storage and connection pooling

2.  **Synchronous/Blocking API:** 
    - **Why:** Simpler to implement and test within timeframe
    - **Trade-off:** Limited throughput under high concurrency (thread-per-request model)
    - **Production Alternative:** Spring WebFlux (reactive) or async processing with message queues (Kafka/RabbitMQ)

3.  **No Authentication/Authorization:** 
    - **Why:** Focused on core business logic (no overdraft rule, concurrency) as requested
    - **Trade-off:** APIs are publicly accessible
    - **Production Alternative:** Spring Security with JWT tokens and role-based access control (RBAC)

## 🔮 Potential Improvements

**With more time, I would implement:**

1.  **API Documentation (Swagger/OpenAPI):**
    - Auto-generated interactive API documentation
    - Contract sharing for frontend teams
    - Try-it-out functionality for easier testing
    - Would use `springdoc-openapi-starter-webmvc-ui` dependency

2.  **Idempotency Keys:**
    - Accept `Idempotency-Key` header in POST requests
    - Store request fingerprints in Redis with TTL
    - Prevents duplicate transactions if client retries due to network failure
    - Critical for financial APIs

3.  **Enhanced Monitoring & Observability:**
    - Integrate Spring Boot Actuator for health checks
    - Add Micrometer metrics (transaction counts, latency percentiles)
    - Distributed tracing with correlation IDs
    - Centralized logging (ELK stack or CloudWatch)

4.  **Advanced Features:**
    - **Daily/Monthly Spending Limits:** Configurable thresholds per card
    - **Card Status Management:** Active/Blocked/Expired states with lifecycle
    - **Currency Support:** Multi-currency cards with exchange rates
    - **Webhooks:** Real-time notifications for transactions

5.  **Performance Optimizations:**
    - Database query optimization with proper indexes
    - Redis caching for frequently accessed cards
    - Connection pooling tuning (HikariCP settings)
    - Rate limiting to prevent abuse

## 🎯 Business Logic Implementation

**No Overdraft Rule (Critical Requirement)**
* **Implementation Location:** `CardServiceImpl.spend()` method (lines 68-72)
* **Logic:** Before deducting any amount, the service checks if `card.getBalance().compareTo(amount) < 0`, and if true, throws `IllegalArgumentException` with "Insufficient funds" message
* **Concurrency Safety:** The `@Version` field ensures that if two transactions happen simultaneously:
  1. Transaction A reads balance = 100
  2. Transaction B reads balance = 100
  3. Transaction A spends 60 (balance = 40, version++, saved)
  4. Transaction B tries to spend 60 → **Fails** due to version mismatch
  5. Client receives HTTP 409 and can retry with fresh balance

**Database Schema Design**
* **Cards Table:** Stores card metadata and current balance with optimistic lock version
* **Card_Transactions Table:** Immutable log of all balance changes with foreign key to cards
* **Why Two Tables?** Separates current state from historical events (CQRS pattern)

## 📚 Additional Learning & Research
To ensure this solution was robust, I researched and applied:
* **JPA Concurrency Control:** Studied the behavior of `@Version` and `ObjectOptimisticLockingFailureException` to prevent race conditions in financial transactions
* **Bean Validation Best Practices:** Ensured monetary validations happen at DTO level before reaching business logic
* **Financial Software Standards:** Researched BigDecimal usage patterns in banking applications to avoid precision loss

### Tools Adopted:

- **Lombok:** Reduces boilerplate for DTOs and entities
- **H2 Database:** In-memory database for development/testing
- **Mockito:** Mocking framework for unit tests
- **JUnit 5:** Modern testing framework with better assertions