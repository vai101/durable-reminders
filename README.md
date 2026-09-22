***

### 2. `README.md`
*(Create a new file named `README.md` in the root of your project and paste this in. It's great practice to have a clean landing page on your GitHub repo.)*

```markdown
# Durable Reminders & Follow-Ups Engine

This is a backend engine designed to handle durable, timezone-aware scheduling for a conversational AI companion. It ensures that scheduled follow-ups are executed exactly once, survive server crashes, respect Daylight Saving Time transitions, and safely handle race conditions if a user edits or cancels a reminder at the exact moment it's being processed.

This project was built for the **Caygnus Product Engineering Challenge**.

## 🚀 Tech Stack
* **Java 21**
* **Spring Boot 3.3.x**
* **Spring Data JPA**
* **Embedded H2 Database** (File-backed for offline durability)
* **HTML/JS** (For a lightweight local testing UI)

## ⚙️ Architecture
The system uses a **Ports and Adapters (Hexagonal)** architecture to strictly isolate the core state machine from the Spring framework and network layers.
* **Optimistic Locking:** Ensures safe handling of concurrent edits/cancellations.
* **Idempotency Keys:** Prevents duplicate notifications during network retries.
* **Virtual Clock:** Allows instant, deterministic testing of time-dependent logic without thread sleeps.

## 🛠️ How to Run Locally

You don't need Maven or a database installed to run this project. 

1. **Start the server:**
   ```bash
   ./mvnw spring-boot:run