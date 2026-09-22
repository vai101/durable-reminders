# Product Engineering Challenge Submission

## Candidate

- **Name:** Vaibhav
- **Email:** anand.vaibhav0@gmail.com
- **GitHub:** https://github.com/vai101/durable-reminders
- **Selected problem:** Problem 3: Durable Reminders and Follow-Ups
- **Demo video:** https://drive.google.com/file/d/1k81UjyvK9hWzGZoDdSvLD6LpEoz6KS-J/view?usp=drive_link

## Run the project

**Prerequisites:** Java 17 or 21. No external database or message broker is required, as the project uses an embedded H2 database. The Maven wrapper is included, so a global Maven installation is not strictly required.

```bash
./mvnw spring-boot:run
```

**Triggering Scenarios:**
*   **Successful scenario:** Navigate to `http://localhost:8080` in your web browser. Use the provided HTML control panel to schedule a new reminder, and click the `+1h` button to advance the injected virtual clock. You will see the background worker instantly detect the overdue work and transition the state to `DELIVERED`.
*   **Failure/Recovery scenario:** Schedule a reminder for tomorrow, then kill the Spring Boot server process in your terminal using `Ctrl+C`. Advance the virtual clock from the UI (it will time out, which is expected) so the reminder becomes overdue, and then run `./mvnw spring-boot:run` to restart the server. The system will immediately query the database upon boot, discover the overdue work that piled up while offline, and process it automatically.

## Run the tests

```bash
./mvnw clean test
```

## Acceptance scenarios and verification

I completed all 7 required acceptance scenarios (AC1-AC7):
1. Scheduled delivery
2. Restart recovery
3. Temporary failure & retry
4. Duplicate execution (Idempotency)
5. Edit before execution
6. Cancellation
7. Time-zone boundary & DST handling

All scenarios are tested completely deterministically using an injected virtual clock without relying on arbitrary thread sleeps.

Provide the exact command or steps used to run the problem-specific verification benchmark:

```bash
./mvnw clean test -Dtest=VerificationBenchmarkTest
```

**Observed Result:**
```text
=== CAYGNUS BENCHMARK OBSERVED RESULTS ===
Total Items Created: 20
State DELIVERED: 14
State CANCELLED: 3
State FAILED:    3
Unique Messages Delivered: 14
Unique Keys Stored:        14
==========================================
```
Zero missing and zero duplicate logical deliveries occurred at the fake notification sink.

**Failure/Recovery Scenario Demonstrated:** In the demo video, I demonstrate a race condition where a user reschedules an item at the exact moment a worker claims it (AC5). This is handled safely via `@Version` optimistic locking; the database rejects the worker's stale update, the delivery attempt is aborted, and the reminder remains safely scheduled for the user's newly requested time.

## Architecture and data flow

The solution is built using a **Ports and Adapters (Hexagonal)** architecture backed by an atomic state machine.
*   **Core Domain:** The `Reminder` entity acts as the aggregate root, holding pure business logic, optimistic locking, and explicit lifecycle rules without Spring-specific execution logic. `ReminderState` dictates that `DELIVERED`, `CANCELLED`, and `FAILED` are strictly terminal states.
*   **Application Services:** `ReminderService` handles synchronous CRUD operations. `DueWorkPoller` operates independently as a `@Scheduled` background worker running a discovery loop.
*   **Ports & Adapters:** `ReminderRepository` connects to the embedded H2 database. The `SettableClock` holds an `AtomicReference<Instant>` that can be artificially advanced. `FakeNotificationClient` tracks processed keys to verify idempotency.
*   **Data Flow:** HTTP requests hit the controller and are synchronously converted to a UTC instant before being saved to the database. Decoupled from this, the background `DueWorkPoller` continuously checks the virtual clock against the database, atomically claims due reminders (applying a 60-second lease), and pushes the payload to the notification client.

## Technology choices

I chose **Java and Spring Boot** backed by an **embedded H2 database**.
*   I chose this stack because `java.time` (JSR-310) provides the most mathematically sound, battle-tested IANA timezone and DST gap/overlap resolution without relying on fragmented third-party libraries. 
*   The embedded H2 database guarantees complete determinism during testing and requires zero infrastructure setup for the reviewer.
*   **Trade-offs accepted:** I accepted the trade-off that relying on a simple polling loop and atomic database queries can experience row-lock contention under massive scale.

## Important decisions

1.  **Optimistic Locking for Concurrency:** I utilized a `@Version` field on the `Reminder` entity to enforce optimistic locking. This prevents race conditions if a user edits or cancels a reminder while the background worker is attempting to deliver it.
2.  **Idempotency Keys Tied to Version:** I tied the external delivery key directly to the specific optimistic lock version of the reminder (e.g., `reminder:UUID:v1`). This creates a unique idempotency key that guarantees duplicate logical notifications are suppressed automatically during network retries, but allows rescheduled items (v2) to fire cleanly.
3.  **Self-Injection Proxy Trap:** Inside the polling loop, I used `@Autowired @Lazy private DueWorkPoller self;` to call internal execution methods. This forces the execution back through Spring's AOP proxy, guaranteeing that the database claim and delivery commit within a strict, atomic `@Transactional` boundary.

## Assumptions and limitations

*   **Row-Lock Contention:** The current atomic claiming query relies on a polling loop that can experience row-lock contention if scaled horizontally to thousands of concurrent nodes.
*   **Claim Leases:** When a worker claims a task, it applies a temporary 60-second lease. If the server crashes mid-delivery, the system assumes the worker failed and the lease must naturally expire before the item is recovered and picked back up by the poller.

## Production and scale

If this prototype needed to operate in production at a significantly greater scale with millions of users, I would evolve it by:
1.  **Pessimistic Skip-Locked Claims:** I would migrate the database to PostgreSQL and modify the polling query to use `SELECT ... FOR UPDATE SKIP LOCKED`. This allows dozens of concurrent worker nodes to claim rows instantly without blocking each other during the polling phase.
2.  **Outbox Pattern:** Instead of having the poller synchronously call the external notification client, the poller would commit an "Outbox Event" to a distributed queue (like AWS SQS or Kafka), decoupling schedule discovery from the highly variable latency of outbound network calls.

## AI usage

I utilized an AI coding assistant primarily for generating boilerplate Maven configuration, DTO records, and scaffolding the Spring Boot controller layers. I manually designed the state machine boundaries, the `java.time` DST logic (`withEarlierOffsetAtOverlap()`), and the `@Version` optimistic locking mechanism to ensure no proxy-bypass bugs compromised the `@Transactional` worker scope. 

## Credibility note

*   **The problem it solved:** At Amazon, the manual enterprise onboarding process had an SLA of 8-10 days, and data transfers from S3 to AWS Redshift required frequent manual on-call intervention.
*   **Your personal contribution:** I engineered a highly scalable onboarding workflow utilizing AWS Step Functions, API Gateway, and Java Lambdas to automate the process to near real-time, alongside a reliable automated data ingestion pipeline.
*   **The scale or operational complexity involved:** The pipeline securely managed and transformed over 10,000 daily enterprise records, ensuring strict data integrity guarantees and eliminating manual bottlenecks at scale
*   **One difficult engineering or product decision:** Deciding to break the monolithic onboarding flow into fully decoupled AWS Step Functions. While this introduced complexity in managing distributed state, it was essential to allow individual failure points to be retried automatically without rolling back the entire multi-day onboarding transaction.
