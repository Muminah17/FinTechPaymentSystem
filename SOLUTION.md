
# Solution Write-Up

## 🎯 Goal
Design and implement a small **payments system** with:
- A **Ledger Service** (single source of truth for accounts & transactions).
- A **Transfer Service** (handles business logic, batching, idempotency).

---

## 🏗️ Architecture

### Ledger Service
- Owns account balances and immutable transaction log.
- Exposes APIs for creating accounts and posting transfers.
- Enforces **single source of truth**: only Ledger can change balances.

### Transfer Service
- Coordinates calls to Ledger Service.
- Provides **idempotent APIs** using the `Idempotency-Key` header.
- Implements **batch transfer API** with parallelization via WebClient.
- Persists idempotency keys in DB with request hash + cached response.

---

## 🔑 Design Trade-offs

### 1. No Distributed Transactions
- Chose service-level **idempotency** instead of distributed 2PC (simpler, more scalable).
- Each service is autonomous, communicating via REST.

### 2. Idempotency Strategy
- **Transfer Service** stores idempotency keys with request hash + response.
- Duplicate calls with same key → return cached response, no double debit.
- **Ledger Service** enforces per-transfer uniqueness by `transferId`.

### 3. Batch Transfers
- Implemented parallel Ledger calls for efficiency.
- Failures in individual transfers are isolated (others can still succeed).
- Entire batch response cached against one `Idempotency-Key`.

### 4. Storage
- Default is in-memory **H2 DB** for simplicity.
- Docker Compose can be extended to include Postgres for realism.

---

## 🧪 Testing
- **Unit Tests**: Service logic & idempotency behavior.
- **Integration Tests**: Full controller flow with MockMvc, validating idempotency for single + batch transfers.
- Ensures that repeated requests with same key return identical responses.

---

## 🚀 Running in Docker
- Multi-stage Dockerfiles for both services (small runtime images).
- `docker-compose.yml` orchestrates both services in one network.
- Transfer Service calls Ledger via container DNS (`http://ledger-service:8080`).

---

## 💡 Possible Extensions
- Add **Postgres** container with Flyway migrations.
- Introduce **Kafka** or event streaming for async transaction events.
- Implement **retry with exponential backoff** for failed ledger calls.
- Harden error codes (INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND, etc.).

---

## 🏁 Summary
This solution demonstrates:
- **Clean separation of concerns** (Ledger = source of truth, Transfer = orchestration).
- **Idempotency & consistency guarantees** without distributed transactions.
- **Scalability** via batch + parallel calls.
- **Deployability** via Docker Compose for a realistic multi-service setup.
