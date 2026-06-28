# NexStore — High-Throughput E-Commerce Inventory Engine

NexStore is a production-ready, concurrent e-commerce backend engineered with **Java 17**, **Spring Boot 3.x**, and **Hibernate ORM**. The system is architected to handle high-density transactional checkout flows, stateful resource allocation, and zero-data-loss image streaming across a cloud-native environment.

🌐 **Live Platform:** [https://nexstore.azurewebsites.net](https://nexstore.azurewebsites.net)

---

## 🛠️ Technical Skills Inventory

* **Languages & Core Mechanics:** Java 17, Multi-threading, Thread Synchronization, Memory Management, Custom Exception Propagation.
* **Frameworks & Framework Internals:** Spring Boot 3.x, Spring MVC, Spring Data JPA, Hibernate ORM, Tomcat Servlet Engine.
* **Cloud Infrastructure & Architecture:** Microsoft Azure (App Service, Dedicated B1 Compute, Flexible PostgreSQL Server, Blob Storage).
* **DevOps & Automation:** Docker, GitHub Actions CI/CD Pipelines, Azure Container Registry (ACR), Git.

---

## 🏗️ Production-Grade Architectural Implementations

### 🔒 Concurrency Control & Race Condition Mitigation
To protect database integrity during high-volume traffic spikes (e.g., flash sales), the engine implements advanced concurrency controls. 
* **Data Integrity:** Employs explicit synchronization blocks and database-level transactional boundaries to prevent dirty reads and data races.
* **Pessimistic Locking:** Utilizes database-level locking modes to serialize inventory updates on highly contested rows, guaranteeing that stock cannot drop below zero even under massive parallel thread execution.

### 🔌 Database Connection Pool Optimization (HikariCP)
To minimize the latency of establishing raw network sockets, the persistence layer uses an optimized **HikariCP** pool configuration:
* Pre-allocates warm connections (`minimum-idle=5`, `maximum-pool-size=10`) to eliminate thread allocation overhead.
* Implements background check queries (`SELECT 1`) to validate connections instantly, preventing thread hangs and cutting request processing times down to milliseconds.

### 📦 Stateless Binary Asset Streaming (Azure Blob Storage)
Bypasses volatile, ephemeral container filesystems to keep the application tier completely stateless:
* Intercepts multi-part binary image payloads cleanly using Spring's `@RequestPart` binding.
* Streams data asynchronously via the **Azure Storage Blob SDK**, assigning cryptographically unique identifiers (`UUID`) to neutralize asset collisions.
* Updates the PostgreSQL table with permanent public URI strings, preserving media data across automated container lifecycles.

---

## 🚀 Automated CI/CD Lifecycle (GitHub Actions)

The repository leverages an automated Continuous Deployment pipeline to push code straight to production without manual script dependencies:

[Push to Main] ➡️ [Maven Test & Package] ➡️ [Docker Multi-Stage Build] ➡️ [Push to ACR] ➡️ [Azure Hot Restart]

1. Build & Validate: An isolated Linux runner compiles source artifacts and runs verification checks (mvn clean package).

2. Containerization: Packages the compiled Java binary into an immutable Docker image layer.

3. Distribution & Deployment: Pushes the finalized image directly to Azure Container Registry (ACR) and triggers a zero-downtime hot swap on the active Web App container instance.
