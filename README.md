# Fintech Payments – Ledger & Transfer Services

This project implements a simplified **payments platform** consisting of two microservices:

- **Ledger Service**  
  Responsible for account management, balances, and recording immutable transactions.

- **Transfer Service**  
  Responsible for coordinating money transfers between accounts, ensuring idempotency, and interacting with the Ledger Service.

For a demo of the payments system please view video [here](https://www.loom.com/share/6fab71ce02614ee2863a5697cabcb0c0?sid=c02cb60e-48fc-4a6b-82fc-b2fefdc4a3dd).

---

## 🚀 Getting Started

### 1. Prerequisites
- Docker & Docker Compose installed (tested with Docker 27.x).
- Java 17 (if running locally without Docker).
- Maven 3.9+ (if building outside Docker).

### 2. Running with Docker Compose
From the project root:

```bash
docker compose up --build
```

Services will be available at:

Ledger Service → http://localhost:8081

Transfer Service → http://localhost:8080

### 3. 📌 Usage Examples

1. Create Accounts
````
   curl -X POST http://localhost:8081/accounts \
   -H "Content-Type: application/json" \
   -d '{"owner":"Alice","balance":500}'
````

````
curl -X POST http://localhost:8081/accounts \
-H "Content-Type: application/json" \
-d '{"owner":"Bob","balance":100}'
````
2. Fetch Account Details

   curl http://localhost:8081/accounts/1

   curl http://localhost:8081/accounts/2

3. Single Transfer (Idempotent)
````
   curl -X POST http://localhost:8080/transfers \
   -H "Content-Type: application/json" \
   -H "Idempotency-Key: tx-12345" \
   -d '{
   "transferId":"t1",
   "fromAccountId":1,
   "toAccountId":2,
   "amount":100
   }'
   ````


👉 Running the same command again with the same Idempotency-Key will return the same response and not double-charge.

4. Batch Transfer
````
   curl -X POST http://localhost:8080/transfers/batch \
   -H "Content-Type: application/json" \
   -H "Idempotency-Key: batch-001" \
   -d '{
   "transfers":[
   {"transferId":"t-b1","fromAccountId":1,"toAccountId":2,"amount":50},
   {"transferId":"t-b2","fromAccountId":2,"toAccountId":1,"amount":25}
   ]
   }'
   ````

5. Simulating Insufficient Funds

If you attempt to debit more than available, Transfer Service will mark the transfer as FAILED:
````

curl -X POST http://localhost:8080/transfers \
-H "Content-Type: application/json" \
-H "Idempotency-Key: tx-999" \
-d '{
"transferId":"t-fail",
"fromAccountId":1,
"toAccountId":2,
"amount":10000
}'
````
### ⚙️ 4. Technologies

Java 17 + Spring Boot 3

Spring Data JPA (H2 by default, easily swappable to Postgres/MySQL)

Spring Web (RestClient & WebClient)

Docker + Docker Compose

JUnit 5 + MockMvc for tests

### 🧪 5. Testing

Run tests locally:
`mvn clean test`

Tests cover:

- Unit tests for services

- Integration tests for controllers

- Idempotency behavior (single + batch transfers)

### 📦 6. Project Structure

````
fintech-payments/
├── ledger-service/     # Ledger microservice
├── transfer-service/   # Transfer microservice
├── docker-compose.yml  # Runs both services together
├── README.md
└── SOLUTION.md         # Design decisions & tradeoffs
````

