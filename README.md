# NexStore — High-Throughput E-Commerce Inventory Engine

NexStore is a production-grade, high-concurrency e-commerce backend built with **Java 17/21**, **Spring Boot 3.x**, and **Hibernate ORM**. The system is engineered to handle highly contested transactional checkout flows, session-scoped resource allocation, and zero-data-loss image streaming across a stateless, cloud-native architecture.

---

## 🛠️ Tech Stack & Production System Architecture

* **Core Backend Stack:** Spring Boot 3.2.4, Spring MVC, Spring Data JPA, Hibernate ORM, Tomcat Servlet Engine.
* **JEE & Concurrency:** Session-scoped state synchronization, Java Monitors, Database Pessimistic Locking (`SELECT FOR UPDATE`), Transaction isolation.
* **Cloud Infrastructure:** Microsoft Azure App Service, Azure Flexible PostgreSQL Server, Azure Blob Storage (Stateless Architecture).
* **DevOps & CI/CD:** Docker (Multi-stage build), GitHub Actions CI/CD Pipeline, Azure Container Registry (ACR).
* **Security & Auth:** Spring Security 6 (RBAC, CSRF, Eager Session Creation), BCrypt Cryptographic Hashing.

---

## 🏗️ Concurrency & Synchronization Architecture

NexStore implements multi-layered concurrency controls to guarantee data consistency during high-traffic spikes (e.g., flash sales) across three different architectural boundaries: the JEE servlet session, the database transaction, and the HTTP request flow.

### 1. Session-Scoped State Synchronization (Tomcat Thread Pool)
In a standard JEE container like Apache Tomcat, each incoming HTTP request is assigned to a thread from a thread pool. When a user updates their cart from multiple browser tabs concurrently, these threads execute parallel mutating actions on the same in-memory cart resource.

* **Mutual Exclusion:** `CartService` is designated as `@SessionScope` (one instance per user session) and serializes state mutations using Java monitor locks. This prevents dirty writes and collection corruption (like `ConcurrentModificationException` on the backing map) within Tomcat's thread pool.
* **Compound Atomicity:** While concurrent collections (like `ConcurrentHashMap`) handle individual read/write safety, they fail to guarantee atomicity across compound operations. Synchronizing mutating operations ensures that check-then-act operations (such as checking if an item exists before incrementing or removing an item when quantity drops to zero) remain atomic.

### 2. Database-Level Pessimistic Locking
While session-scoped synchronization protects memory state for a single user, it cannot protect shared database records (like product inventory stock levels) from race conditions across different users. 

* **Pessimistic Write Locking (`SELECT FOR UPDATE`):** If two users attempt to purchase the last unit of a product concurrently under standard `READ_COMMITTED` transaction isolation, the system is vulnerable to a lost update anomaly. NexStore resolves this by acquiring a pessimistic write lock on the product row. This blocks concurrent transactions attempting to read or update that specific product until the owning transaction commits.
* **Transactional Atomicity & Rollbacks:** The stock verification and deduction are wrapped in a Spring `@Transactional` context. If any single item in the order fails the stock validation, the entire transaction throws a runtime exception, triggering a full database rollback and ensuring that stock is never partially deducted or corrupted.

### 3. Cart Snapshot Tamper Verification
To prevent the checkout process from using a stale or altered cart state (e.g., if a user modifies their cart in Tab A while submitting a checkout from Tab B), NexStore uses a signed Cart Snapshot Signature. The server calculates and signs a cryptographic string representation of the cart's content during page load. During checkout submission, the server recalculates the signature of the current session cart and validates it against the submitted token. A mismatch aborts the request, securing the transaction against mid-session cart tampering.

---

## ⚡ Hibernate ORM Performance Tuning: Solving the N+1 Query Problem

In Spring Data JPA, fetching an entity with lazy relationships (like an `Order` which has a `@OneToMany` collection of `OrderItem`) can lead to the N+1 Query Problem. 

* **The Problem:** Rendering the order history dashboard requires fetching $N$ orders. In a naive implementation, Hibernate executes 1 query to fetch the orders, followed by $N$ subsequent queries to fetch the line items as the rendering engine iterates through the collection. This degrades response times and saturates the database connection pool.
* **The Solution (JPA Entity Graph):** NexStore optimizes this by specifying a JPA Entity Graph on the order fetch methods. This instructs Hibernate to perform a single query with an outer join, pulling the parent orders, their line items, and the nested product details simultaneously. This reduces database traffic from $O(N)$ down to a single network roundtrip.

---

## ☁️ Stateless Container Design & Cloud-Native Storage

To allow horizontal scaling across cloud instances (such as Azure App Service Scale-Outs), the application server layer must remain completely stateless. Saving uploaded product images to the local filesystem is a major architectural anti-pattern in cloud-native environments:
* Local directories are ephemeral; restarting or scaling a container destroys all uploaded assets.
* Uploaded files on Instance A are inaccessible to users hitting Instance B behind the load balancer.

### Azure Blob Storage Integration
NexStore bypasses the local filesystem entirely. Multipart file uploads are processed directly in memory as byte streams and uploaded straight to Azure Blob Storage via the Spring Cloud Azure SDK.
* **Cryptographic Naming:** File names are converted to unique UUIDs to prevent key collision overwrites.
* **CDNs and Persistence:** The database stores only the permanent CDN URL string, leaving the container filesystem free of persistent state and ready for auto-scaling.

### HikariCP Connection Pool Optimization
To handle database connection allocations under high-density concurrent load, the application persists database links using optimized HikariCP pool configurations:
* **Pool Ceiling:** Establishes a maximum pool size to prevent overloading the PostgreSQL database with idle sockets.
* **Connection Timeout:** Enforces connection timeout limits to throw exceptions immediately if a connection request is blocked, preventing Tomcat threads from hanging indefinitely.

---

## 🔒 Enterprise Security Architecture

NexStore implements Spring Security 6 to construct robust security barriers:
* **Eager Session Creation:** Configured session creation policies to eager state to resolve session commitment issues before Thymeleaf renders security templates.
* **Role-Based Access Control (RBAC):** Admin endpoints are secured using explicit authority checks. This matches the roles fetched from the database by the custom user details service without stripping standard role prefixes.
* **No Default Plaintext Credentials:** The system uses BCrypt password hashing. Default admin credentials are dynamically fetched from the environment. If no variable is set, the system generates a cryptographically random initial password and logs it, preventing hardcoded credentials in the repository.

---

## 🚀 Continuous Deployment Pipeline (GitHub Actions & Docker)

NexStore implements automated Git-ops via a Continuous Integration and Deployment pipeline.

### Containerization Strategy
The application is packaged using a two-stage build to isolate compilation tools and yield a highly secure, minimal container runtime footprint:
1. **Compilation Stage:** Uses a Maven environment to compile source artifacts and packaging files.
2. **Lightweight JRE Stage:** Copies only the final compiled artifact into a lightweight JRE image, minimizing the container's attack surface and footprint.

### CI/CD Workflow
Upon a commit push to the main branch:
1. **Setup & Verification:** A virtual GitHub runner initializes the JDK environment and runs tests.
2. **Docker Packaging:** Packages the application with a unique, cache-busting image tag based on date, time, and git hash.
3. **Publishing to ACR:** Authenticates and pushes the Docker image to the private Azure Container Registry (ACR).
4. **Azure Web App Hot Reload:** Signals Azure Web App to deploy the new container tag and triggers a clean restart.
