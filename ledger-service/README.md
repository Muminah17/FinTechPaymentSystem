# Ledger Service

The **Ledger Service** is the system of record for accounts and transactions.  
It manages account balances, records immutable ledger entries, and enforces business rules such as insufficient funds.

---

## 🚀 Running Locally

### 1. With Maven
```
mvn clean spring-boot:run
```
Service will start on http://localhost:8081
.

### 2. With Docker

From the project root:

docker compose up ledger-service --build


Service will be available at http://localhost:8081
 (exposed via docker-compose).

## 📌 API Endpoints

Create Account
```
POST /accounts
Content-Type: application/json

{
"owner": "Alice",
"balance": 500
}
```

Get Account
```
GET /accounts/{id}
```
Apply Transfer

```
POST /ledger/transfer
Content-Type: application/json

{
"transferId": "t1",
"fromAccountId": 1,
"toAccountId": 2,
"amount": 100
}
```

## ⚙️ Tech

* Java 17 + Spring Boot 3

* Spring Data JPA (H2 in-memory DB by default)

* REST API with Spring Web

* Dockerized via multi-stage Dockerfile

## 🧪 Testing

```
mvn test
```

Covers:

- Account creation & retrieval

- Transfers (success + insufficient funds)

- Transaction immutability