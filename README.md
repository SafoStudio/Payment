# Payment

Double-entry ledger system with GraphQL API.

## 📌 About

Financial engine that stores money, processes transfers, and guarantees that no transaction is lost or duplicated.

### Features:
- ✅ Creates wallets for users
- ✅ Processes transfers with double-entry accounting
- ✅ Maintains an immutable journal of all operations
- ✅ Protects against double spending (idempotency)
- ✅ Provides full transaction audit trail

### Tech Stack:
- Java 21
- Spring Boot 4.0.3
- Spring GraphQL
- Spring Data JDBC
- PostgreSQL 17
- Flyway migrations
- Docker & Docker Compose


## ⚡ Quick Start

```bash
# Copy .env file
cp .env.example .env

# Start database
docker-compose up -d

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local